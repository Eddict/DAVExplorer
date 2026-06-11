package com.ms.xml.dso;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.ms.xml.om.Document;
import com.ms.xml.om.Element;
import com.ms.xml.om.ElementEnumeration;
import com.ms.xml.om.ElementFactory;
import com.ms.xml.parser.ParseException;
import com.ms.xml.util.Name;
import com.ms.xml.util.StringInputStream;
import com.ms.xml.util.XMLOutputStream;

import java.net.URI;

public class XMLDSO {

    // Instance variables (kept from original)
    private Object myDSL;
    private Document document;
    private Document schema;
    private Element schemaRoot;
    private String schemaParam;
    private String error;
    private URL url;
    private boolean loaded;
    private boolean inlineXML;
    static Vector providers = new Vector();
    private URL documentBase; // Replaces the Applet's inherent document base

    /**
     * Construct a new XMLDSO object with an empty document.
     */
    public XMLDSO() {
        document = new Document();
        inlineXML = false;
        loaded = false;
    }

    /**
     * Standard main method entry point replacing the Applet container.
     */
    public static void main(String[] args) {

        String urlArg = (args.length > 0) ? args[0] : null;
        String idArg = (args.length > 1) ? args[1] : null;
        String schemaArg = (args.length > 2) ? args[2] : null; // Extract the 3rd argument
        new XMLDSO().startLogic(urlArg, idArg, schemaArg);
    }

    public void setDocumentBase(URL base) {
        this.documentBase = base;
    }

    /**
     * The startLogic method replaces the old init() method.
     */
    public void startLogic(String urlArg, String idArg, String schemaArg) {
        // NOTE: Because this is no longer an Applet, getParameter() is undefined.
        // You will need to replace getParameter() with args passed from main(String[] args),
        // environment variables, or a properties file.
        String arg = urlArg;

        if (arg != null && arg.length() > 0) {
            load(arg);
        } else {
            loaded = true;
            // Or we can get the XML data inline from inside the
            // <APPLET> tag using the JavaScript object model.
            arg = idArg;
            if (arg != null && arg.length() > 0) {
                Object appl = null;

                // JSObject bridge logic was already commented out in your source
                if (appl != null) {
                    // data parsing logic (commented out in your source)
                } else {
                    setError("Error finding <APPLET> with ID=" + arg);
                }
            } else {
                setError("No PARAM with NAME=URL was found, "
                        + "so perhaps you specified the XML inside the APPET tag "
                        + "but in order for this to work you must also specify "
                        + "<PARAM NAME=\"APPLET\" VALUE=\"xmldso\">"
                        + " where the value matches the ID of your APPLET tag.");
            }
        }
        updateSchema();
    }

    /**
     * This method is called whenever the document is changed, that is, from the
     * load and setroot methods. You can also call this method if you have
     * manually changed the document or the SCHEMA <PARAM>.
     */
    public void updateSchema() {
        schema = new Document();

        // Use the instance variable instead of getParameter()
        if (this.schemaParam != null && this.schemaParam.length() > 0) {
            schema = parseXML(this.schemaParam);
            schemaRoot = schema.getRoot();
        } else {
            // Generate schema from the data.
            if (document != null && document.getRoot() != null) {
                Element root = document.getRoot();
                SchemaNode n = new SchemaNode(schema, schema, root.getTagName());
                generateSchema(root, n);
                // remember only first entry below root
                ElementEnumeration iter = new ElementEnumeration(schema.getRoot(),
                        XMLRowsetProvider.nameROWSET, Element.ELEMENT);
                schemaRoot = (Element) iter.nextElement();
            }
        }
        notifyListeners();
    }

    public Document parseXML(String xml) {
        Document _document = new Document();
        if (xml == null) {
            return _document;
        }

        try {
            _document.load(new StringInputStream(xml));
        } catch (Exception e) {
            setError("Caught exception parsing given XML.  "
                    + e.toString());
        }
        return _document; // but return what we have anyway.
    }

    /**
     * Parse and Process the given update gram
     */
    public void handleUpdateString(String xml) {
        Document d = parseXML(xml);
        handleUpdate(d.getRoot());
    }

    /**
     * Process the given update gram
     */
    public void handleUpdate(Element root) {
        if (document == null) {
            document = new Document();
            document.addChild(root, null);
            updateSchema();
            return;
        }

        try {
            Element docRoot = document.getRoot();
            for (ElementEnumeration en = new ElementEnumeration(root);
                    en.hasMoreElements();) {
                Element action = (Element) en.nextElement();
                updateTree(docRoot, action);
            }
        } catch (Exception e) {
            setError("HandleUpdate error: " + e.toString());
        }
    }

    boolean updateTree(Element node1, Element node2) {
        int type = node2.getType();
        if (type == Element.COMMENT || type == Element.PI || type == Element.WHITESPACE) {
            return true;
        }

        if (type == Element.PCDATA || type == Element.ENTITYREF) {
            if (node1.getText().equals(node2.getText())) {
                return true;
            } else {
                return false;
            }
        }

        String action = (String) node2.getAttribute("UPDATE-ACTION");
        if (action != null && (action.equalsIgnoreCase("APPEND")
                || action.equalsIgnoreCase("INSERT"))) {
            {
                node2.removeAttribute("UPDATE-ACTION");
                boolean append = action.equalsIgnoreCase("APPEND");
                if (append) {
                    node1.addChild(node2, null);
                } else {
                    node1.addChild(node2, 0, 0);
                }
                notifyNewRow(node1, append);
                return true;
            }
        }
        int num = node2.numElements();
        if (num > 0) {
            int row = 0;
            for (ElementEnumeration en = new ElementEnumeration(node1, node2.getTagName(), Element.ELEMENT);
                    en.hasMoreElements();) {
                row++;
                Element element = (Element) en.nextElement();

                // make sure we match all the child patterns.
                Vector actions = new Vector();
                boolean match = false;
                for (ElementEnumeration en2 = new ElementEnumeration(node2);
                        en2.hasMoreElements();) {
                    Element pattern = (Element) en2.nextElement();
                    String cmd = (String) pattern.getAttribute("UPDATE-ACTION");
                    if (cmd == null || cmd.equalsIgnoreCase("APPEND")
                            || cmd.equalsIgnoreCase("INSERT")
                            || cmd.equalsIgnoreCase("REMOVE")) {
                        match = updateTree(element, pattern);
                        if (!match) {
                            break; // found a non-match
                        }
                    } else {
                        actions.addElement(pattern);
                    }
                }
                if (!match) {
                    continue;
                }

                if (action != null && action.equalsIgnoreCase("REMOVE")) {
                    node1.removeChild(element);
                    notifyRemovedRow(node1, row);
                    return true;
                }

                // since we matched let's process the actions.
                for (Enumeration an = actions.elements(); an.hasMoreElements();)
                {
                    Element actElement = (Element)an.nextElement();
                    String cmd = (String)actElement.getAttribute("UPDATE-ACTION");
                    actElement.removeAttribute("UPDATE-ACTION");
                    if (cmd.equalsIgnoreCase("REPLACE") )
                    {
                        ElementEnumeration ee = new ElementEnumeration(element,
                                actElement.getTagName(),Element.ELEMENT);
                        Element oldChild = (Element)ee.nextElement();

                        element.addChild(actElement,oldChild);
                        if (oldChild != null)
                            element.removeChild(oldChild);
                        element.setParent(node1);
                        notifyCellChanged(row, element, actElement);
                    }
                }
                return true;
            }
        }
        return false;
    }

    int getColumn(Element _schema, Element element) {
        int col = 0;
        String tagname = element.getTagName().toString();
        for (Enumeration en = _schema.getElements(); en.hasMoreElements();) {
            col++;
            Element e = (Element) en.nextElement();
            String n1 = e.getAttribute("NAME").toString();
            if (n1.equals(tagname)) {
                return col;
            }
        }
        setError(tagname + " not found in schema.");

        return 0;
    }

    /**
     * Function to provide the OLE-DB simple provider interface to callers. The
     * qualifier parameter is ignored at this point, but is available to allow
     * the applet to serve up more than one data set
     */
//    public OLEDBSimpleProvider msDataSourceObject(String qualifier)
//    {
//        removeProvider(myProvider);
//        if (document != null && document.getRoot() != null && schemaRoot != null) {
//            // This is a smarter provider that supports a hierarchy
//            // of XMLRowsetProviders based on the given schema information.
//            myProvider = new XMLRowsetProvider (document.getRoot(), schemaRoot, (ElementFactory)document, null);
//        } else {
//            myProvider = null;
//        }
//        addProvider(myProvider);
//        return myProvider;
//    }
    /**
     * This is a standard method for DSO's.
     */
//    public void addDataSourceListener(DataSourceListener  listener)
//        throws java.util.TooManyListenersException
//    {
//        if (myDSL != null) {
//            com.ms.com.ComLib.release(myDSL);
//        }
//        myDSL = listener;
//    }
    /**
     * This is a standard method for DSO's.
     */
//    public void removeDataSourceListener( DataSourceListener   listener)
//    {
//        // BUGBUG: Shouldn't have to call release here. This is a
//        //         bug in the VM implementation.
//        if (myDSL != null) {
//            com.ms.com.ComLib.release(myDSL);
//        }
//        myDSL = null;
//    }
    void removeProvider(XMLRowsetProvider p) {
        if (p != null) {
            providers.removeElement(p);
        }
    }

    void addProvider(XMLRowsetProvider p) {
        if (p != null) {
            providers.addElement(p);
        }
    }

    /**
     * Notify all DSO's that are bound to this row that the given cell has been
     * modified.
     */
    void notifyCellChanged(int row, Element rowElement, Element modifiedElement) {
        long count = 0;
        for (Enumeration en = providers.elements(); en.hasMoreElements();) {
            XMLRowsetProvider provider = (XMLRowsetProvider) en.nextElement();
            Element node = rowElement;
            while (node != null) {
                XMLRowsetProvider child = provider.findChildProvider(node);
                if (child != null) {
                    count++;
//                    OLEDBSimpleProviderListener listener = child.getListener();
//                    if (listener != null)
//                    {
//                        int col = getColumn(child.getSchema(), modifiedElement);
//                        if (col != 0) {
//                            listener.aboutToChangeCell(row,col);
//                            listener.cellChanged(row,col);
//                            return;
//                        }
//                    }
                }
                node = node.getParent();
            }
        }
    }

    void notifyNewRow(Element node, boolean append) {
        for (Enumeration en = providers.elements(); en.hasMoreElements();) {
            XMLRowsetProvider provider = (XMLRowsetProvider) en.nextElement();
            XMLRowsetProvider child = provider.findChildProvider(node);
            if (child != null) {
//                OLEDBSimpleProviderListener listener = child.getListener();
//                if (listener != null)
//                {
//                    int row = (append) ? provider.getRowCount() : 1;
//                    try {
//                        listener.aboutToInsertRows(row,1);
//                    } catch (com.ms.com.ComSuccessException e)
//                    { }
//                    listener.insertedRows(row,1);
//                }
            }
        }
    }

    void notifyRemovedRow(Element node, int row) {
        for (Enumeration en = providers.elements(); en.hasMoreElements();) {
            XMLRowsetProvider provider = (XMLRowsetProvider) en.nextElement();
            XMLRowsetProvider child = provider.findChildProvider(node);
            if (child != null) {
//               OLEDBSimpleProviderListener listener = child.getListener();
//               if (listener != null)
//               {
//                    try {
//                        listener.aboutToDeleteRows(row,1);
//                    } catch (com.ms.com.ComSuccessException e)
//                    { }
//                    listener.deletedRows(row,1);
//                }
            }
        }
    }

    /**
     * Notify current DataSouceListener that data has changed.
     */
    public void notifyListeners() {
        if (myDSL != null) {
            try {
//                myDSL.dataMemberChanged("");
            } catch (Exception e) {
                setError("Error notifying data members changed: " + e.toString());
            }
        }
    }

    /**
     * Return the loaded document.
     */
    public Object getDocument() {
        // return document to allow scripting
        return document;
    }

    /**
     * This method is called to set the root of the document for this XMLDSO
     * object. This is useful when you want to link multiple DSO's together.
     */
    public void setRoot(Element e) {
        document = new Document();
        document.addChild(e, null);
        updateSchema();
    }

    public void clear() {
        document = new Document();
        updateSchema();
    }

    /**
     * Reload the document using the given URL. Relative URL's are resolved
     * using the HTML document as a base URL.
     */
    public void load(String arg) {
        loaded = false;
        try {
            // Use our new class-level variable instead of the Applet method
            if (this.documentBase == null) {
                // Assume arg is a fully qualified URL or absolute file path
                url = new URI(arg).toURL();
            } else {
                // Resolve relative to our defined base
                URI base = new URI(this.documentBase.toString());
                URI relative = new URI(arg);
                URI resolved = base.resolve(relative).normalize();
                url = resolved.toURL();
            }

            document.load(url.toString());
            loaded = true;
        } catch (Exception e) {
            setError("Error loading XML document '" + arg + "'.  " + e.toString());
        }

        if (loaded && schema != null) {
            updateSchema();
        }
    }

// Change String callback to Consumer<Document> so Java can handle the return
    public void asyncLoad(String arg, java.util.function.Consumer<Document> callback) {
        document = null;
        try {
            URL _url;
            if (this.documentBase == null) {
                _url = new URI(arg).toURL();
            } else {
                URI base = new URI(this.documentBase.toString());
                URI relative = new URI(arg);
                URI resolved = base.resolve(relative).normalize();
                _url = resolved.toURL();
            }

            document = new Document();

            // Remove 'win' entirely. Pass the functional callback to your thread.
            XMLParserThread t = new XMLParserThread(_url, callback);
            t.start();

        } catch (Exception e) {
            setError("Error loading document '" + arg + "'.  " + e.toString());
        }
    }

    /**
     * Return the XML for the loaded document as a big string. Pass in the
     * formatting style. 0=default, 1=pretty, 2=compact.
     */
    public Object getXML(int style) {
        // return XML as long string
        if (document != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                XMLOutputStream s = new XMLOutputStream(out);
                s.setOutputStyle(style);
                document.save(s);
            } catch (Exception e) {
                setError(e.toString());
            }
            return out.toString();
        }
        return null;
    }

    /**
     * Return string containing last error encountered by the XMLDSO.
     */
    public String getError() {
        return error;
    }

    /**
     * Save the schema to the given file.
     */
    public void saveSchema(String filename) {
        if (schema != null) {
            try {
                FileOutputStream out = new FileOutputStream(filename);
                schema.save(new XMLOutputStream(out));
            } catch (Exception e) {
                setError(e.toString());
            }
        }
    }

    /**
     * Return the schema as a string.
     */
    public Object getSchema(int style) {
        // return Schema as long string
        if (schema != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                XMLOutputStream xout = new XMLOutputStream(out);
                xout.setOutputStyle(style);
                schema.save(xout);
            } catch (Exception e) {
                setError(e.toString());
            }
            return out.toString();
        }
        return null;
    }

    /**
     * Save the XML document to the given file.
     */
    public void save(String filename) {
        if (document != null) {
            try {
                FileOutputStream out = new FileOutputStream(filename);
                document.save(document.createOutputStream(out));
            } catch (Exception e) {
                setError(e.toString());
            }
        }
    }

    private void setError(String e) {
        error = e;
        System.err.println("XMLDSO Error: " + e);
    }

    void generateSchema(Element source, SchemaNode n) {
//        System.out.println("Processing node:" + source.getTagName());
        // go thru the children of source and generate schema
        // nodes added later to parent.
        for (ElementEnumeration en = new ElementEnumeration(source); en.hasMoreElements();) {
            Element e = (Element) en.nextElement();
            int t = e.getType();
            Name name = e.getTagName();
//            System.out.println("Found: " + name + "," + t);
            if (t == Element.ELEMENT) {
                // now we know that parent is a rowset...
                SchemaNode sn = n.setRow(name);
                generateSchema(e, sn);
            }
        }

        // if children didn't create element it means it is a column...
        // (at least for now.  If we find that it is supposed to be
        // a ROWSET later in the document, then setRow() will fix it up).
        if (n.e == null) {
            n.createElement(false);
        }
    }
}

//------------------------------------------------------------------
class SchemaNode {

    Element e;
    Map<String, SchemaNode> rows = new HashMap<>();
    Name name;
    Element parent;
    ElementFactory factory;

    SchemaNode(Element parent, ElementFactory factory, Name name) {
        this.parent = parent;
        this.name = name;
        this.factory = factory;
    }

    void createElement(boolean rowset) {
        if (rowset) {
            e = factory.createElement(null, Element.ELEMENT, XMLRowsetProvider.nameROWSET, null);
        } else {
            e = factory.createElement(null, Element.ELEMENT, XMLRowsetProvider.nameCOLUMN, null);
        }
        parent.addChild(e, null);
        e.setAttribute(XMLRowsetProvider.nameNAME, name.toString());
    }

    SchemaNode setRow(Name name) {
        if (e == null) {
            createElement(true);
        } else if (e.getTagName() != XMLRowsetProvider.nameROWSET) {
            // We have now discovered that node is supposed to
            // be a ROWSET not a COLUMN.
            parent.removeChild(e);
            createElement(true);
        }

        SchemaNode sn = getRow(name);
        // add new subnode if new or was added as text
        if (sn == null) {
            sn = new SchemaNode(e, factory, name);
            addRow(name, sn);
        }
        return sn;
    }

    void addRow(Name _name, SchemaNode n) {
        rows.put(_name.toString(), n);
    }

    SchemaNode getRow(Name n) {
        return rows.get(n.toString());
    }
}

//------------------------------------------------------------------
//class XMLRowsetProvider  implements OLEDBSimpleProvider
class XMLRowsetProvider {

    public XMLRowsetProvider(Element e, Element schema, ElementFactory f, XMLRowsetProvider parent) {
        root = e;
        this.schema = schema;
        this.parent = parent;
        this.factory = f;
        // This provider iterates over nodes in root that match
        // ROWSET name defined in schema.
        rowset = Name.create((String) schema.getAttribute(nameNAME));
        resetIterator();
    }

    public Element getSchema() {
        return schema;
    }

    void resetIterator() {
        iter = new ElementEnumeration(root, rowset, Element.ELEMENT);
        row = (Element) iter.nextElement();
        rowindex = 0;
    }

    public int getRowCount() {
        // Return number of children in root that match the
        // ROWSET name.
        int result = 0;
        iter.reset();
        while (iter.nextElement() != null) {
            result++;
        }
        iter.reset();
        row = (Element) iter.nextElement();
        rowindex = 0;
        return result;
    }

    public int getEstimatedRows() {
        return getRowCount();
    }

    public int getColumnCount() {
        // Simply return the number of elements in the schema.
        int columns = schema.getChildren().getLength();
        return columns;
    }

    public int getRWStatus(int iRow, int iColumn) {
        return 1;
    }

//    public void addOLEDBSimpleProviderListener(OLEDBSimpleProviderListener l)
//    {
//        if (listener != null) {
//            com.ms.com.ComLib.release(listener);
//        }
//        listener = l;
//    }
//    public void removeOLEDBSimpleProviderListener(OLEDBSimpleProviderListener l)
//    {
//        if (listener != null) {
//            com.ms.com.ComLib.release(listener);
//        }
//        listener = null;
//    }
    public int find(int iStartRow, int iColumn, Object varSearchVal,
            int findFlags, int compType) {
        return 0;
    }

    public int deleteRows(int iRow, int cRows) {
//        try {
//            if (listener != null) listener.aboutToDeleteRows(iRow,cRows);
//        } catch (com.ms.com.ComSuccessException e)
//        { }
        int result = 0;
        for (int i = iRow; i < iRow + cRows; i++) {
            Element _row = getRow(iRow);
            if (_row != null) {
                result++;
                root.removeChild(_row);
                removeChildProvider(_row);
            }
        }
        resetIterator();
//        if (listener != null) listener.deletedRows(iRow,cRows);
        return result;
    }

    public int insertRows(int iRow, int cRows) {
//        try {
//            if (listener != null) listener.aboutToInsertRows(iRow,cRows);
//        } catch (com.ms.com.ComSuccessException e)
//        { }

        Name name = Name.create(schema.getAttribute("NAME").toString());
        for (int i = iRow; i < iRow + cRows; i++) {
            Element newRow = factory.createElement(null, Element.ELEMENT, name, null);
            Element previousRow = root.getChildren().getChild(i);
            root.addChild(newRow, previousRow);
        }
        resetIterator();
//        if (listener != null) listener.insertedRows(iRow,cRows);
        return cRows;
    }

    public Object getVariant(int iRow, int iColumn, int formatType) {
        Object retVal = null;
        if (iRow == 0) {
            // return the column information.
            if (iColumn <= getColumnCount()) {
                Element e = schema.getChildren().getChild(iColumn - 1);
                String name = (String) e.getAttribute(nameNAME);
                if (name != null) {
                    if (e.getTagName() != nameCOLUMN) {
                        // HACK to mark the column as rowset for the OSP...
                        retVal = "^" + name + "^";
                    } else {
                        retVal = name;
                    }
                }
            }
        } else {
            getRow(iRow); // update current row.
            retVal = getColumn(row, iColumn - 1);
        }
        return retVal;
    }

    Element getRow(int iRow) {
        // The current row is cached for performance reasons.
        if (rowindex != iRow - 1) {
            if (rowindex == iRow - 2) {
                // next row in sequence.
                rowindex++;
                row = (Element) iter.nextElement();
            } else {
                // caller just did a random jump, so we need to resync
                // the iterator.
                iter.reset();
                row = (Element) iter.nextElement();
                rowindex = 0;
                for (int i = 0; i < iRow - 1; i++) {
                    row = (Element) iter.nextElement();
                    rowindex++;
                }
            }
        }
        return row;
    }

    public Object getColumn(Element _row, int col) {
        Element se = schema.getChildren().getChild(col);
        Name name = Name.create((String) se.getAttribute(nameNAME));
        if (se.getTagName() == nameCOLUMN) {
            Element child = findChild(_row, name);
            if (child != null) {
                String text = child.getText();
                return text;
            }
            return null;
        } else {
            // Must be a rowset, so return a rowset provider.
            // First see if we've already created one for this row.
            XMLRowsetProvider value = findChildProvider(_row);
            if (value == null) {
                // Have to create a new rowset provider then.
                value = new XMLRowsetProvider(_row, se, factory, this);
                addChildProvider(value);
            }
            return value;
        }
    }

    public XMLRowsetProvider findChildProvider(Element _row) {
        if (childProviders != null) {
            for (Enumeration en = childProviders.elements(); en.hasMoreElements();) {
                XMLRowsetProvider child = (XMLRowsetProvider) en.nextElement();
                if (child.root == _row) {
                    return child;
                }
            }
        }
        return null;
    }

    void addChildProvider(XMLRowsetProvider child) {
        if (childProviders == null) {
            childProviders = new Vector();
        }
        childProviders.addElement(child);
    }

    void removeChildProvider(Element _row) {
        XMLRowsetProvider value = findChildProvider(_row);
        if (value != null) {
            childProviders.removeElement(value);
        }
    }

    /**
     * Recurrsively search given row for first child or grand-child node with
     * matching tag name.
     */
    public Element findChild(Element _row, Name tag) {
        for (ElementEnumeration en = new ElementEnumeration(_row, null, Element.ELEMENT);
                en.hasMoreElements();) {
            Element child = (Element) en.nextElement();
            if (child.getType() == Element.ELEMENT && child.getTagName() == tag) {
                return child;
            } else if (child.numElements() > 0) {
                child = findChild(child, tag);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    public void setVariant(int iRow, int iColumn, int formatType, Object var) {
        getRow(iRow); // update current row.
        if (row == null) {
            return; // non-existant row.

        }
        Element se = schema.getChildren().getChild(iColumn - 1);
        if (se.getTagName() == nameCOLUMN) {
            String attr = (String) se.getAttribute(nameNAME);
            if (attr != null) {
                Name name = Name.create(attr);
                Element child = findChild(row, name);
                if (child == null) {
                    child = factory.createElement(null, Element.ELEMENT, name, null);
                    row.addChild(child, null); // order doesn't actually matter.
                }
                if (child != null) {
                    if (child.numElements() == 0) {
                        child.addChild(factory.createElement(null, Element.PCDATA, null, null), null);
                    }

                    if (child.numElements() == 1
                            && child.getChild(0).getType() == Element.PCDATA) {
//                        if (listener != null) listener.aboutToChangeCell(iRow,iColumn);
                        Element pcdata = child.getChild(0);
                        String text = (String) var;
                        pcdata.setText(text);
//                        if (listener != null) listener.cellChanged(iRow,iColumn);
                    }
                    // otherwise ignore the setVariant !
                }
            }
        }
    }

//    public OLEDBSimpleProviderListener getListener()
//    {
//        return listener;
//    }
    public String getLocale() {
        // nothing special here. Return the default locale, ie. US
        return "";
    }

    public int isAsync() {
        return 0;
    }

    public void stopTransfer() {
    }

    Element root;
    Element schema;
    Element row;
    ElementEnumeration iter;
    int rowindex;
    Name rowset;
    XMLRowsetProvider parent;
//    OLEDBSimpleProviderListener listener;
    Object listener = null;
    ElementFactory factory;
    Vector childProviders;

    static Name nameCOLUMN = Name.create("COLUMN");
    static Name nameROWSET = Name.create("ROWSET");
    static Name nameNAME = Name.create("NAME");
    static Name nameATTRIBUTE = Name.create("ATTRIBUTE");
    static Name nameVALUE = Name.create("VALUE");
}

//---------------------------------------------------------------------
class XMLParserThread extends Thread {

    URL url;
    //	JSObject win;
    Object win;
    String callback;

    //	XMLParserThread(URL url, JSObject win, String callback)
    XMLParserThread(URL url, Object win) {
        this.url = url;
        this.win = win;
        this.callback = callback;
    }

    public void run() {
        Object args[] = new Object[2];
        try {
            Document document = new Document();
            document.load(url);
            args[0] = "ok";
            args[1] = document;
//			win.call(callback, args);
        } catch (ParseException e) {
            args[0] = e.toString();
            args[1] = null;
//            win.call(callback, args);
        }
    }
}
