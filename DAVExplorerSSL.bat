@echo off
java -Dhttps.protocols=TLSv1.2,TLSv1.3 -jar -DSSL=yes DAVExplorer.jar
