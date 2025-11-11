#!/usr/bin/env python3
"""
Script de ejemplo para usar la API de Firma Digital
Requiere: pip install requests
"""

import requests
import base64
import json
from pathlib import Path
from typing import Dict, Optional


class FirmaDigitalAPI:
    """Cliente para la API de Firma Digital"""
    
    def __init__(self, base_url: str = "http://localhost:8080/api"):
        """
        Inicializa el cliente de la API
        
        Args:
            base_url: URL base de la API
        """
        self.base_url = base_url
        self.version_token = "NDQuMS4w"  # "4.1.0" en base64
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/x-www-form-urlencoded'
        })
    
    def validar_version(self) -> str:
        """
        Valida la versión de la API
        
        Returns:
            Respuesta de la API
        """
        url = f"{self.base_url}/version"
        data = {'base64': self.version_token}
        
        response = self.session.post(url, data=data)
        response.raise_for_status()
        return response.text
    
    def obtener_fecha_hora(self) -> str:
        """
        Obtiene la fecha y hora del servidor
        
        Returns:
            Fecha y hora en formato ISO-8601
        """
        url = f"{self.base_url}/fecha-hora"
        data = {'base64': self.version_token}
        
        response = self.session.post(url, data=data)
        response.raise_for_status()
        return response.text
    
    def validar_certificado(
        self,
        jwt_token: str,
        certificado_path: str,
        password: str
    ) -> Dict:
        """
        Valida un certificado digital PKCS#12
        
        Args:
            jwt_token: Token JWT de autenticación
            certificado_path: Ruta al archivo .p12
            password: Contraseña del certificado
            
        Returns:
            Resultado de la validación
        """
        url = f"{self.base_url}/appvalidarcertificadodigital"
        
        # Leer y codificar el certificado
        with open(certificado_path, 'rb') as f:
            cert_base64 = base64.b64encode(f.read()).decode('utf-8')
        
        data = {
            'jwt': jwt_token,
            'pkcs12': cert_base64,
            'password': password,
            'base64': self.version_token
        }
        
        response = self.session.post(url, data=data)
        response.raise_for_status()
        
        try:
            return response.json()
        except json.JSONDecodeError:
            return {'resultado': response.text}
    
    def firmar_documento(
        self,
        jwt_token: str,
        certificado_path: str,
        password: str,
        documento_path: str,
        metadata: Optional[Dict] = None
    ) -> Dict:
        """
        Firma un documento digital
        
        Args:
            jwt_token: Token JWT de autenticación
            certificado_path: Ruta al archivo .p12
            password: Contraseña del certificado
            documento_path: Ruta al documento a firmar
            metadata: Metadatos de la firma (razón, localización, cargo)
            
        Returns:
            Resultado de la firma con el documento firmado
        """
        url = f"{self.base_url}/appfirmardocumento"
        
        # Leer y codificar el certificado
        with open(certificado_path, 'rb') as f:
            cert_base64 = base64.b64encode(f.read()).decode('utf-8')
        
        # Leer y codificar el documento
        with open(documento_path, 'rb') as f:
            doc_base64 = base64.b64encode(f.read()).decode('utf-8')
        
        # Metadatos por defecto
        if metadata is None:
            metadata = {
                'razon': 'Firma digital',
                'localizacion': 'Ecuador',
                'cargo': 'Firmante'
            }
        
        data = {
            'jwt': jwt_token,
            'pkcs12': cert_base64,
            'password': password,
            'documento': doc_base64,
            'json': json.dumps(metadata),
            'base64': self.version_token
        }
        
        response = self.session.post(url, data=data)
        response.raise_for_status()
        
        try:
            return response.json()
        except json.JSONDecodeError:
            return {'resultado': response.text}
    
    def verificar_documento(
        self,
        jwt_token: str,
        documento_firmado_path: str
    ) -> Dict:
        """
        Verifica la firma de un documento
        
        Args:
            jwt_token: Token JWT de autenticación
            documento_firmado_path: Ruta al documento firmado
            
        Returns:
            Resultado de la verificación
        """
        url = f"{self.base_url}/appverificardocumento"
        
        # Leer y codificar el documento
        with open(documento_firmado_path, 'rb') as f:
            doc_base64 = base64.b64encode(f.read()).decode('utf-8')
        
        data = {
            'jwt': jwt_token,
            'documento': doc_base64,
            'base64': self.version_token
        }
        
        response = self.session.post(url, data=data)
        response.raise_for_status()
        
        try:
            return response.json()
        except json.JSONDecodeError:
            return {'resultado': response.text}
    
    def verificar_certificado_revocado(self, serial: int) -> bool:
        """
        Verifica si un certificado está revocado
        
        Args:
            serial: Número de serie del certificado
            
        Returns:
            True si está revocado, False si no
        """
        url = f"{self.base_url}/certificado/revocado/{serial}"
        
        response = self.session.get(url)
        response.raise_for_status()
        
        return response.text.lower() == 'true'
    
    def obtener_fecha_revocacion(self, serial: int) -> Optional[str]:
        """
        Obtiene la fecha de revocación de un certificado
        
        Args:
            serial: Número de serie del certificado
            
        Returns:
            Fecha de revocación o None si no está revocado
        """
        url = f"{self.base_url}/certificado/fechaRevocado/{serial}"
        
        response = self.session.get(url)
        response.raise_for_status()
        
        fecha = response.text
        return fecha if fecha.lower() != 'null' else None
    
    def guardar_documento_firmado(
        self,
        documento_base64: str,
        ruta_salida: str
    ) -> None:
        """
        Guarda un documento firmado desde Base64
        
        Args:
            documento_base64: Documento en Base64
            ruta_salida: Ruta donde guardar el archivo
        """
        documento_bytes = base64.b64decode(documento_base64)
        
        with open(ruta_salida, 'wb') as f:
            f.write(documento_bytes)
        
        print(f"Documento guardado en: {ruta_salida}")


def ejemplo_uso():
    """Ejemplo de uso del cliente de la API"""
    
    # Inicializar cliente
    api = FirmaDigitalAPI("http://localhost:8080/api")
    
    # Tu token JWT (debes obtenerlo de tu sistema de autenticación)
    jwt_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    
    print("=" * 60)
    print("API de Firma Digital - Ejemplos de Uso")
    print("=" * 60)
    
    # 1. Validar versión
    print("\n1. Validando versión de la API...")
    try:
        version = api.validar_version()
        print(f"   ✓ Versión: {version}")
    except Exception as e:
        print(f"   ✗ Error: {e}")
    
    # 2. Obtener fecha y hora
    print("\n2. Obteniendo fecha y hora del servidor...")
    try:
        fecha = api.obtener_fecha_hora()
        print(f"   ✓ Fecha y hora: {fecha}")
    except Exception as e:
        print(f"   ✗ Error: {e}")
    
    # 3. Verificar estado de certificado
    print("\n3. Verificando estado de certificado...")
    try:
        serial = 123456789
        revocado = api.verificar_certificado_revocado(serial)
        print(f"   ✓ Certificado {serial} revocado: {revocado}")
    except Exception as e:
        print(f"   ✗ Error: {e}")
    
    # 4. Validar certificado (requiere archivos)
    # print("\n4. Validando certificado digital...")
    # try:
    #     resultado = api.validar_certificado(
    #         jwt_token=jwt_token,
    #         certificado_path="certificado.p12",
    #         password="MiContraseña123"
    #     )
    #     print(f"   ✓ Resultado: {resultado}")
    # except Exception as e:
    #     print(f"   ✗ Error: {e}")
    
    # 5. Firmar documento (requiere archivos)
    # print("\n5. Firmando documento...")
    # try:
    #     metadata = {
    #         "razon": "Firma de aprobación",
    #         "localizacion": "Quito, Ecuador",
    #         "cargo": "Director"
    #     }
    #     
    #     resultado = api.firmar_documento(
    #         jwt_token=jwt_token,
    #         certificado_path="certificado.p12",
    #         password="MiContraseña123",
    #         documento_path="documento.pdf",
    #         metadata=metadata
    #     )
    #     
    #     if resultado.get('resultado') == 'OK':
    #         # Guardar documento firmado
    #         api.guardar_documento_firmado(
    #             resultado['documentoFirmado'],
    #             'documento_firmado.pdf'
    #         )
    #         print("   ✓ Documento firmado exitosamente")
    #     else:
    #         print(f"   ✗ Error: {resultado.get('mensaje')}")
    # except Exception as e:
    #     print(f"   ✗ Error: {e}")
    
    # 6. Verificar documento firmado (requiere archivos)
    # print("\n6. Verificando documento firmado...")
    # try:
    #     resultado = api.verificar_documento(
    #         jwt_token=jwt_token,
    #         documento_firmado_path="documento_firmado.pdf"
    #     )
    #     print(f"   ✓ Resultado: {resultado}")
    # except Exception as e:
    #     print(f"   ✗ Error: {e}")
    
    print("\n" + "=" * 60)
    print("Ejemplos completados")
    print("=" * 60)


if __name__ == "__main__":
    ejemplo_uso()
