# DAV Explorer

[![Build](https://github.com/Eddict/DAVExplorer/actions/workflows/build.yml/badge.svg)](https://github.com/Eddict/DAVExplorer/actions/workflows/build.yml)

DAV Explorer is a WebDAV and DeltaV client application that supports the WebDAV,
DeltaV, and ACL protocols. It provides a tree view of a WebDAV server, resource
upload/download, locking, property editing, versioning, and ACL management.

Version: **1.00**

---

## Prerequisites

- **JDK 11 or later** (JDK 17 LTS recommended). JDK 8 also works.
- `make` (Linux/macOS) — available via your system package manager.
- On Windows: no `make` required; use the provided batch scripts instead.

---

## Building on Linux / macOS

```bash
# Clone the repository
git clone https://github.com/Eddict/DAVExplorer.git
cd DAVExplorer

# Compile sources and produce DAVExplorer.jar
make
```

The build compiles all Java sources and moves the resulting `DAVExplorer.jar`
to the repository root.

---

## Building on Windows

The top-level `Makefile` **does not work on Windows**. Use the provided batch
scripts instead:

```bat
rem Compile all sources and produce DAVExplorer.jar
make.bat
```

To build only the jar (assumes sources are already compiled):

```bat
rem Create DAVExplorer.jar from compiled class files
DAVjar.bat
```

---

## Producing the jar only (Linux/macOS)

If the sources are already compiled and you only need to repackage the jar:

```bash
cd classes
make DAVExplorer.jar
mv DAVExplorer.jar ..
```

Or use the provided shell script from the root:

```bash
./DAVjar.sh
```

---

## Running DAV Explorer

### Normal mode

```bash
java -jar DAVExplorer.jar
```

Or use the provided launch scripts:

- **Linux/macOS:** `./DAVExplorer.sh`
- **Windows:** `DAVExplorer.bat`

### SSL / HTTPS mode

```bash
java -DSSL=yes -jar DAVExplorer.jar
```

Or use the SSL launch scripts:

- **Linux/macOS:** `./DAVExplorerSSL.sh`
- **Windows:** `DAVExplorerSSL.bat`

SSL support can also be enabled at runtime from the **Edit** menu.

---

## Command-line options

| Option | Description |
|---|---|
| `-Dhelp` or `-Dhelp=yes` | Print all available options |
| `-DSSL=yes` | Enable SSL/HTTPS mode |
| `-Ddebug=all\|request\|response\|treeview\|treenode\|fileview` | Enable function traces |
| `-Dpropfind=allprop` | Use `<allprop>` tag in PROPFIND |
| `-DSharePoint=yes` | Enable SharePoint server workaround |
| `-DApache=yes` | Enable Apache 1.3.x PROPPATCH workaround |
| `-Dlocal=no` | Disable local directory structure view |
| `-Dcompress=no` | Disable accepting compressed data |

---

## Project layout

```
DAVExplorer/
├── classes/           Java source tree and sub-Makefiles
│   ├── HTTPClient/    HTTPClient library sources
│   ├── com/ms/xml/    MS XML parser sources
│   └── edu/uci/ics/DAVExplorer/  Main application sources
├── Makefile           Top-level build file (Linux/macOS)
├── make.bat           Top-level build script (Windows)
├── DAVjar.bat         Jar-only build script (Windows)
├── DAVjar.sh          Jar-only build script (Linux/macOS)
├── DAVExplorer.sh     Launch script (Linux/macOS)
├── DAVExplorerSSL.sh  SSL launch script (Linux/macOS)
├── DAVExplorer.bat    Launch script (Windows)
├── DAVExplorerSSL.bat SSL launch script (Windows)
├── swingall.jar       Bundled Swing library (legacy; not needed on JDK 2+)
├── INSTALL.TXT        Original installation instructions (historical)
├── Readme.txt         Original project overview (historical)
└── License.txt        Apache-style license
```

---

## License

DAV Explorer is released under an Apache-style license. See [License.txt](License.txt) for details.

Originally developed at the University of California, Irvine.
