# JRemapper
An easy to use GUI for remapping classes, methods, and fields of compiled java programs.

### How to use:

**Loading a file**: 

1. Run the executable jar
2. From the File menu, load a jar _(Creates a listing of class files)_
3. Select a class file from the tree menu on the left _(This will decompile it with CFR)_

**Renaming a class**:

1. Click the class name _(The title bar should change, notifying which class you have selected)_
2. Delete the class name and retype a new name _(Internal names are used, so for packages you would use `com/example/MyClass`)_
3. Press enter

**Renaming a field or method**:

1. Click the method or field _(The title bar should change, notifying which member you have selected)_
2. Type a new name
3. Press enter

**More tips***:

* Middle click a tab you want to close
* Right click a class to bring up a menu *(Currently only lets you open the class in a new tab)*

### Libraries used:
* [BMF](https://github.com/Col-E/Bytecode-Modification-Framework) - _Remapping abilities_
* [CFR](http://www.benf.org/other/cfr/) - _Java decompiler_
* [RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea) - _Syntax highlighting_

### Images

![Screenshot](shot.png)