; Instalador de ComercioControl (Inno Setup 6)
; Empaqueta la app-image generada por jpackage (target\dist\ComercioControl)
; y agrega una página para ingresar la clave de licencia (opcional).
;
; Compilar:  ISCC installer\ComercioControl.iss
; (requiere Inno Setup 6 instalado: https://jrsoftware.org/isdl.php)

#define AppName "ComercioControl"
#define AppVersion "1.0"
#define AppPublisher "Iván Rodríguez"
#define AppExe "ComercioControl.exe"

[Setup]
AppId={{A7F3C2E1-9B4D-4E6A-8C1F-2D5E7A9B0C3D}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
SourceDir=..
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
OutputDir=installer\Output
OutputBaseFilename=ComercioControl-Setup-{#AppVersion}
SetupIconFile=src\main\resources\img\icono.ico
UninstallDisplayIcon={app}\{#AppExe}
WizardStyle=modern
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "es"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "Crear un acceso directo en el escritorio"; GroupDescription: "Accesos directos:"

[Files]
Source: "target\dist\ComercioControl\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{group}\Desinstalar {#AppName}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "Ejecutar {#AppName}"; Flags: nowait postinstall skipifsilent

[Code]
var
  KeyPage: TInputQueryWizardPage;

procedure InitializeWizard();
begin
  KeyPage := CreateInputQueryPage(wpSelectDir,
    'Activación de licencia (opcional)',
    'Si compraste la versión Pro, ingresa tu licencia',
    'Puedes dejar estos campos vacíos para usar la versión gratuita (Lite). ' +
    'Podrás activar la versión Pro más tarde desde el módulo «Mi Negocio».');
  KeyPage.Add('Titular de la licencia:', False);
  KeyPage.Add('Clave (formato CCPRO-XXXXX-XXXXX-XXXXX-XXXXX):', False);
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  Titular, Clave, Dir, Contenido: String;
begin
  if CurStep = ssPostInstall then
  begin
    Titular := Trim(KeyPage.Values[0]);
    Clave := Trim(KeyPage.Values[1]);
    if (Titular <> '') and (Clave <> '') then
    begin
      Dir := ExpandConstant('{userappdata}\ComercioControl');
      ForceDirectories(Dir);
      Contenido := 'titular=' + Titular + #13#10 + 'clave=' + Clave + #13#10;
      SaveStringToFile(Dir + '\activacion.properties', Contenido, False);
    end;
  end;
end;
