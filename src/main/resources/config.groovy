app.mode = config

pkcs11.libraries = [
        'c:/windows/system32/cmP11.dll',
        'c:/windows/system32/aetpkss1.dll',
        'c:/WINDOWS/system32/eTPKCS11.dll',
        'c:/windows/system32/dkck201.dll',
        'C:/Program Files (x86)/Gemalto/IDGo 800 PKCS#11/IDPrimePKCS1164.dll',
        'C:/Program Files/Gemalto/IDGo 800 PKCS#11/IDPrimePKCS11.dll',
]
pkcs11.library = null
pkcs11.slot = null
pkcs11.name = 'token'

signature.certificate.type = 'a1'
signature.certificate.alias = null
signature.certificate.filename = null
signature.reason = 'Seguranca'
signature.location = 'Sao Paulo/SP'
signature.folder.input = './'
signature.folder.output = './signed'