option explicit
dim wshell
set wshell = createobject("wscript.shell")
wshell.run "javaw -Dhttps.protocols=TLSv1.2,TLSv1.3 -jar -DSSL=yes DAVExplorer.jar", 0, false