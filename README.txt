Licorne Survivor

Compiler et lancer (Java 21+) :
  mkdir out
  javac -encoding UTF-8 -d out $(find src -name '*.java')      (Linux / Mac)
  java -cp out org.java.ressources.LicorneSurvivor

Sous Windows (PowerShell) :
  javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName

Dans IntelliJ / Eclipse : ouvrir le dossier "src" comme racine des sources.

Créer un installateur Windows (.exe) :
  1. Installer un JDK 21 (pas seulement un JRE) et l'ajouter au PATH.
  2. Installer WiX Toolset 3.x si `jpackage` indique qu'il est manquant.
  3. Depuis PowerShell, dans le dossier du projet, exécuter :
       Set-ExecutionPolicy -Scope Process Bypass
       .\build-exe.ps1
  4. Le fichier `.exe` est créé dans `dist\installer`.

Le script compile toutes les sources, crée `LicorneSurvivor.jar`, puis utilise
`jpackage` pour inclure un runtime Java dans l'installateur. Le joueur final
n'a donc pas besoin d'installer Java.
