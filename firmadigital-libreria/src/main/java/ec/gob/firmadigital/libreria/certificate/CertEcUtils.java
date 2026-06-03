/*
 * Copyright (C) 2020 
 * Authors: Ricardo Arguello, Misael Fernández
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.*
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ec.gob.firmadigital.libreria.certificate;

import ec.gob.firmadigital.libreria.certificate.ec.alphatechnologies.CertificadoSubjAlphaTechnologiesImpl;
import ec.gob.firmadigital.libreria.certificate.ec.alphatechnologies.CertificadoAlphaTechnologiesImpl;
import ec.gob.firmadigital.libreria.certificate.ec.alphatechnologies.AlphaTechnologiesSubCaCert20242032;
import ec.gob.firmadigital.libreria.certificate.ec.alphatechnologies.AlphaTechnologiesSubCaCert20232026;
import java.security.cert.X509Certificate;

import ec.gob.firmadigital.libreria.certificate.ec.CertificadoFuncionarioPublico;
import ec.gob.firmadigital.libreria.certificate.ec.CertificadoMiembroEmpresa;
import ec.gob.firmadigital.libreria.certificate.ec.CertificadoPersonaJuridica;
import ec.gob.firmadigital.libreria.certificate.ec.CertificadoPersonaNatural;
import ec.gob.firmadigital.libreria.certificate.ec.CertificadoRepresentanteLegal;
import ec.gob.firmadigital.libreria.certificate.ec.CertificadoSelladoTiempo;
import ec.gob.firmadigital.libreria.certificate.ec.alphatechnologies.*;
import ec.gob.firmadigital.libreria.certificate.ec.anfac.*;
import ec.gob.firmadigital.libreria.certificate.ec.argosdata.*;
import ec.gob.firmadigital.libreria.certificate.ec.bce.*;
import ec.gob.firmadigital.libreria.certificate.ec.cj.*;
import ec.gob.firmadigital.libreria.certificate.ec.corpnewbest.*;
import ec.gob.firmadigital.libreria.certificate.ec.datil.*;
import ec.gob.firmadigital.libreria.certificate.ec.digercic.*;
import ec.gob.firmadigital.libreria.certificate.ec.eclipsoft.*;
import ec.gob.firmadigital.libreria.certificate.ec.firmasegura.*;
import ec.gob.firmadigital.libreria.certificate.ec.lazzate.*;
import ec.gob.firmadigital.libreria.certificate.ec.letmi.CertificadoLetmiFactory;
import ec.gob.firmadigital.libreria.certificate.ec.letmi.CertificadoSubjLetmiImpl;
import ec.gob.firmadigital.libreria.certificate.ec.letmi.LetmiSubCaCert20252035;
import ec.gob.firmadigital.libreria.certificate.ec.appfirmas.CertificadoAppFirmasFactory;
import ec.gob.firmadigital.libreria.certificate.ec.appfirmas.CertificadoSubjAppFirmasImpl;
import ec.gob.firmadigital.libreria.certificate.ec.appfirmas.AppFirmasSubCaCert20252050;
import ec.gob.firmadigital.libreria.certificate.ec.securitydata.*;
import ec.gob.firmadigital.libreria.certificate.ec.uanataca.*;
import ec.gob.firmadigital.libreria.certificate.to.DatosUsuario;
import ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException;
import ec.gob.firmadigital.libreria.utils.Utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validar diferentes certificados digitales acreditados por ARCOTEL
 *
 * @author Misael Fernandez
 */
public class CertEcUtils {

    private static final Logger LOGGER = Logger.getLogger(CertEcUtils.class.getName());

    public static final String BCE_NAME = "BANCO CENTRAL DEL ECUADOR";
    public static final String CJ_NAME = "CONSEJO DE LA JUDICATURA";
    public static final String SECURITYDATA_NAME = "SECURITY DATA";
    public static final String ANFAC_NAME = "ANFAC";
    public static final String DIGERCIC_NAME = "DIRECCIÓN GENERAL DE REGISTRO CIVIL, IDENTIFICACIÓN Y CEDULACIÓN";
    public static final String UANATACA_NAME = "UANATACA S.A.";
    public static final String ECLIPSOFT_NAME = "ECLIPSOFT S.A.";
    public static final String DATIL_NAME = "DATILMEDIA S.A.";
    public static final String AGOSDATA_NAME = "ARGOSDATA CA";
    public static final String LAZZATE_NAME = "LAZZATE CIA. LTDA";
    public static final String ALPHATECHNOLOGIES_NAME = "ALPHA TECHNOLOGIES CIA. LTDA.";
    public static final String CORPNEWBEST_NAME = "CORPNEWBEST CIA. LTDA.";
    public static final String FIRMASEGURA_NAME = "FIRMASEGURA S.A.S.";
    public static final String LETMI_NAME = "LETMI ECUADOR S.A.";
    public static final String APPFIRMAS_NAME = "APPFIRMAS S.A.";

    public static X509Certificate getRootCertificate(X509Certificate certificado) throws EntidadCertificadoraNoValidaException {
        String entidadCertStr = getNombreCA(certificado);

        switch (entidadCertStr) {
            case BCE_NAME: {
                try {
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new BceSubCaCert20112021())) {
                        LOGGER.log(Level.FINE,"BceSubCaCert 2011-2021");
                        return new BceSubCaCert20112021();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new BceSubCaCert20192029())) {
                        LOGGER.log(Level.FINE,"BceSubCaCert 2019-2029");
                        return new BceSubCaCert20192029();
                    }
                    return null;
                } catch (java.security.InvalidKeyException ex) {
                    //TODO
                }
            }
            case SECURITYDATA_NAME: {
                try {
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new SecurityDataSubCaCert20112026())) {
                        LOGGER.log(Level.FINE,"SecurityDataSubCaCert");
                        return new SecurityDataSubCaCert20112026();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new SecurityDataSubCaCert20192031())) {
                        LOGGER.log(Level.FINE,"SecurityDataSubCaCert 2019-2031");
                        return new SecurityDataSubCaCert20192031();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new SecurityDataSubCaCert20202039())) {
                        LOGGER.log(Level.FINE,"SecurityDataSubCaCert 2020-2032");
                        return new SecurityDataSubCaCert20202039();
                    }
                    return null;
                } catch (java.security.InvalidKeyException ex) {
                    //TODO
                }
            }
            case CJ_NAME:
                return new ConsejoJudicaturaSubCaCert();
            case ANFAC_NAME:
                try {
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new AnfAc18332SubCaCert20162032())) {
                        LOGGER.log(Level.FINE,"Anf 2016-2032");
                        return new AnfAc18332SubCaCert20162032();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new AnfAc37442SubCaCert20192029())) {
                        LOGGER.log(Level.FINE,"Anf 2019-2029");
                        return new AnfAc37442SubCaCert20192029();
                    }
                    return null;
                } catch (java.security.InvalidKeyException ex) {
                    //TODO
                }
            case DIGERCIC_NAME: {
                return new DigercicSubCaCert20212031();
            }
            case UANATACA_NAME: {
                try {
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new UanatacaSubCaCert0120162029())) {
                        LOGGER.log(Level.FINE,"Uanataca 2016-2029");
                        return new UanatacaSubCaCert0120162029();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new UanatacaSubCaCert0220162029())) {
                        LOGGER.log(Level.FINE,"Uanataca 2016-2029");
                        return new UanatacaSubCaCert0220162029();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new UanatacaSubCaCert0320212034())) {
                        LOGGER.log(Level.FINE,"Uanataca 2021-2034");
                        return new UanatacaSubCaCert0320212034();
                    }
                    return null;
                } catch (java.security.InvalidKeyException ex) {
                    //TODO
                }
            }
            case DATIL_NAME: {
                return new DatilSubCaCert20212031();
            }
            case AGOSDATA_NAME: {
                return new ArgosDataSubCaCert();
            }
            case LAZZATE_NAME: {
                try {
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new LazzateSubCaCert())) {
                        LOGGER.log(Level.FINE,"LazzateCA 2022-2037");
                        return new LazzateSubCaCert();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new LazzateSubCa1Cert())) {
                        LOGGER.log(Level.FINE,"LazzateCA1 2023-2053");
                        return new LazzateSubCa1Cert();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new LazzateSubCa2Cert())) {
                        LOGGER.log(Level.FINE,"LazzateCA2 2023-2053");
                        return new LazzateSubCa2Cert();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new LazzateSubCaWeGoCert())) {
                        LOGGER.log(Level.FINE,"LazzateCAWeGo 2023-2053");
                        return new LazzateSubCaWeGoCert();
                    }
                } catch (java.security.InvalidKeyException ex) {
                    //TODO
                }
            }
            case CORPNEWBEST_NAME: {
                try {
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new CorpNewBestSubCa1_20232033Cert())) {
                        LOGGER.log(Level.FINE,"CorpNewBestSubCa1Cert");
                        return new CorpNewBestSubCa1_20232033Cert();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new CorpNewBestSubCa2_20232033Cert())) {
                        LOGGER.log(Level.FINE,"CorpNewBestSubCa2Cert");
                        return new CorpNewBestSubCa2_20232033Cert();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new CorpNewBestSubCa3_20232033Cert())) {
                        LOGGER.log(Level.FINE,"CorpNewBestSubCa3Cert");
                        return new CorpNewBestSubCa3_20232033Cert();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new CorpNewBestSubCa1_2024011020330619Cert())) {
                        LOGGER.log(Level.FINE,"CorpNewBestSubCa1_2024011020330619Cert");
                        return new CorpNewBestSubCa1_2024011020330619Cert();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new CorpNewBestSubCa2_2024011020330619Cert())) {
                        LOGGER.log(Level.FINE,"CorpNewBestSubCa2_2024011020330619Cert");
                        return new CorpNewBestSubCa2_2024011020330619Cert();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new CorpNewBestSubCa3_2024011020330619Cert())) {
                        LOGGER.log(Level.FINE,"CorpNewBestSubCa3_2024011020330619Cert");
                        return new CorpNewBestSubCa3_2024011020330619Cert();
                    }
                    return null;
                } catch (java.security.InvalidKeyException ex) {
                    //TODO
                }
            }
            case ALPHATECHNOLOGIES_NAME: {
                try {
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new AlphaTechnologiesSubCaCert20232026())) {
                        LOGGER.log(Level.FINE,"AlphaTechnologiesSubCaCert 2023-2026");
                        return new AlphaTechnologiesSubCaCert20232026();
                    }
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new AlphaTechnologiesSubCaCert20242032())) {
                        LOGGER.log(Level.FINE,"AlphaTechnologiesSubCaCert 2024-2032");
                        return new AlphaTechnologiesSubCaCert20242032();
                    }
                    return null;
                } catch (java.security.InvalidKeyException ex) {
                    //TODO
                }
            }
            case FIRMASEGURA_NAME: {
                try {
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new FirmaSeguraSubCaCert20232043())) {
                        LOGGER.log(Level.FINE,"FirmaSeguraSubCaCert2023-2043");
                        return new FirmaSeguraSubCaCert20232043();
                    }
                    return null;
                } catch (java.security.InvalidKeyException ex) {
                    //TODO
                }
            }
            case LETMI_NAME: {
                try {
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new LetmiSubCaCert20252035())) {
                        LOGGER.log(Level.FINE,"LetmiSubCaCert2025-2035");
                        return new LetmiSubCaCert20252035();
                    }
                    return null;
                } catch (java.security.InvalidKeyException ex) {
                    //TODO
                }
            }
            case APPFIRMAS_NAME: {
                try {
                    if (ec.gob.firmadigital.libreria.utils.Utils.verifySignature(certificado, new AppFirmasSubCaCert20252050())) {
                        LOGGER.log(Level.FINE,"AppFirmasSubCaCert2025-2050");
                        return new AppFirmasSubCaCert20252050();
                    }
                    return null;
                } catch (java.security.InvalidKeyException ex) {
                    //TODO
                }
            }
            default:
                throw new EntidadCertificadoraNoValidaException("Entidad Certificadora no reconocida");
        }
    }

    public static String getNombreCA(X509Certificate certificado) {
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(BCE_NAME)) {
            return BCE_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(SECURITYDATA_NAME)) {
            return SECURITYDATA_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(CJ_NAME)) {
            return CJ_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(ANFAC_NAME)) {
            return ANFAC_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(DIGERCIC_NAME)) {
            return DIGERCIC_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(UANATACA_NAME)) {
            return UANATACA_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(DATIL_NAME)) {
            return DATIL_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(AGOSDATA_NAME)) {
            return AGOSDATA_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(LAZZATE_NAME)) {
            return LAZZATE_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(ALPHATECHNOLOGIES_NAME)) {
            return ALPHATECHNOLOGIES_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(CORPNEWBEST_NAME)) {
            return CORPNEWBEST_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(FIRMASEGURA_NAME)) {
            return FIRMASEGURA_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(LETMI_NAME)) {
            return LETMI_NAME;
        }
        if (certificado.getIssuerX500Principal().getName().toUpperCase().contains(APPFIRMAS_NAME)) {
            return APPFIRMAS_NAME;
        }
        return "Entidad no reconocida " + certificado.getIssuerDN().getName();
    }

    public static DatosUsuario getDatosUsuarios(X509Certificate certificado) throws EntidadCertificadoraNoValidaException {
        DatosUsuario datosUsuario = new DatosUsuario();
        if (CertificadoBancoCentralFactory.esCertificadoDelBancoCentral(certificado)) {
            CertificadoBancoCentral certificadoBancoCentral = CertificadoBancoCentralFactory.construir(certificado);
            if (certificadoBancoCentral instanceof CertificadoFuncionarioPublico certificadoFuncionarioPublico) {
                datosUsuario.setCedula(certificadoFuncionarioPublico.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoFuncionarioPublico.getNombres());
                datosUsuario.setApellido(certificadoFuncionarioPublico.getPrimerApellido() + " "
                        + certificadoFuncionarioPublico.getSegundoApellido());
                datosUsuario.setInstitucion(certificadoFuncionarioPublico.getInstitucion());
                datosUsuario.setCargo(certificadoFuncionarioPublico.getCargo());
            }
            if (certificadoBancoCentral instanceof CertificadoMiembroEmpresa certificadoMiembroEmpresa) {
                datosUsuario.setCedula(certificadoMiembroEmpresa.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoMiembroEmpresa.getNombres());
                datosUsuario.setApellido(certificadoMiembroEmpresa.getPrimerApellido() + " "
                        + certificadoMiembroEmpresa.getSegundoApellido());
                datosUsuario.setCargo(certificadoMiembroEmpresa.getCargo());
            }
            if (certificadoBancoCentral instanceof CertificadoPersonaJuridica certificadoPersonaJuridica) {
                datosUsuario.setCedula(certificadoPersonaJuridica.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaJuridica.getNombres());
                datosUsuario.setApellido(certificadoPersonaJuridica.getPrimerApellido() + " "
                        + certificadoPersonaJuridica.getSegundoApellido());
                datosUsuario.setCargo(certificadoPersonaJuridica.getCargo());
            }
            if (certificadoBancoCentral instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido() + " "
                        + certificadoPersonaNatural.getSegundoApellido());
            }
            if (certificadoBancoCentral instanceof CertificadoRepresentanteLegal certificadoRepresentanteLegal) {
                datosUsuario.setCedula(certificadoRepresentanteLegal.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoRepresentanteLegal.getNombres());
                datosUsuario.setApellido(certificadoRepresentanteLegal.getPrimerApellido() + " "
                        + certificadoRepresentanteLegal.getSegundoApellido());
                datosUsuario.setCargo(certificadoRepresentanteLegal.getCargo());
            }
            if (certificadoBancoCentral instanceof CertificadoSelladoTiempo) {
                datosUsuario.setCertificadoDigitalValido(true);
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoConsejoJudicaturaDataFactory.esCertificadoDelConsejoJudicatura(certificado)) {
            CertificadoConsejoJudicatura certificadoConsejoJudicatura = CertificadoConsejoJudicaturaDataFactory.construir(certificado);
            if (certificadoConsejoJudicatura instanceof CertificadoDepartamentoEmpresaConsejoJudicatura certificadoDepartamentoEmpresaConsejoJudicatura) {
                datosUsuario.setCedula(certificadoDepartamentoEmpresaConsejoJudicatura.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoDepartamentoEmpresaConsejoJudicatura.getNombres());
                datosUsuario.setApellido(certificadoDepartamentoEmpresaConsejoJudicatura.getPrimerApellido() + " "
                        + certificadoDepartamentoEmpresaConsejoJudicatura.getSegundoApellido());
                datosUsuario.setCargo(certificadoDepartamentoEmpresaConsejoJudicatura.getCargo());
            }
            if (certificadoConsejoJudicatura instanceof CertificadoEmpresaConsejoJudicatura certificadoEmpresaConsejoJudicatura) {
                datosUsuario.setCedula(certificadoEmpresaConsejoJudicatura.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoEmpresaConsejoJudicatura.getNombres());
                datosUsuario.setApellido(certificadoEmpresaConsejoJudicatura.getPrimerApellido() + " "
                        + certificadoEmpresaConsejoJudicatura.getSegundoApellido());
                datosUsuario.setCargo(certificadoEmpresaConsejoJudicatura.getCargo());
            }
            if (certificadoConsejoJudicatura instanceof CertificadoMiembroEmpresaConsejoJudicatura certificadoMiembroEmpresaConsejoJudicatura) {
                datosUsuario.setCedula(certificadoMiembroEmpresaConsejoJudicatura.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoMiembroEmpresaConsejoJudicatura.getNombres());
                datosUsuario.setApellido(certificadoMiembroEmpresaConsejoJudicatura.getPrimerApellido() + " "
                        + certificadoMiembroEmpresaConsejoJudicatura.getSegundoApellido());
                datosUsuario.setCargo(certificadoMiembroEmpresaConsejoJudicatura.getCargo());
            }
            if (certificadoConsejoJudicatura instanceof CertificadoPersonaJuridicaPrivadaConsejoJudicatura certificadoPersonaJuridicaPrivadaConsejoJudicatura) {
                datosUsuario.setCedula(certificadoPersonaJuridicaPrivadaConsejoJudicatura.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaJuridicaPrivadaConsejoJudicatura.getNombres());
                datosUsuario.setApellido(certificadoPersonaJuridicaPrivadaConsejoJudicatura.getPrimerApellido() + " "
                        + certificadoPersonaJuridicaPrivadaConsejoJudicatura.getSegundoApellido());
                datosUsuario.setCargo(datosUsuario.getCargo());
            }
            if (certificadoConsejoJudicatura instanceof CertificadoPersonaJuridicaPublicaConsejoJudicatura certificadoPersonaJuridicaPublicaConsejoJudicatura) {
                datosUsuario.setCedula(certificadoPersonaJuridicaPublicaConsejoJudicatura.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaJuridicaPublicaConsejoJudicatura.getNombres());
                datosUsuario.setApellido(certificadoPersonaJuridicaPublicaConsejoJudicatura.getPrimerApellido() + " "
                        + certificadoPersonaJuridicaPublicaConsejoJudicatura.getSegundoApellido());
                datosUsuario.setCargo(certificadoPersonaJuridicaPublicaConsejoJudicatura.getCargo());
            }
            if (certificadoConsejoJudicatura instanceof CertificadoPersonaNaturalConsejoJudicatura certificadoPersonaNaturalConsejoJudicatura) {
                datosUsuario.setCedula(certificadoPersonaNaturalConsejoJudicatura.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaNaturalConsejoJudicatura.getNombres());
                datosUsuario.setApellido(certificadoPersonaNaturalConsejoJudicatura.getPrimerApellido() + " "
                        + certificadoPersonaNaturalConsejoJudicatura.getSegundoApellido());
            }
            if (certificadoConsejoJudicatura instanceof CertificadoSelladoTiempo) {
                datosUsuario.setCertificadoDigitalValido(true);
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoSecurityDataFactory.esCertificadoDeSecurityData(certificado)) {
            CertificadoSecurityData certificadoSecurityData = CertificadoSecurityDataFactory.construir(certificado);
            if (certificadoSecurityData instanceof CertificadoFuncionarioPublico certificadoFuncionarioPublico) {
                datosUsuario.setCedula(certificadoFuncionarioPublico.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoFuncionarioPublico.getNombres());
                datosUsuario.setApellido(certificadoFuncionarioPublico.getPrimerApellido() + " "
                        + certificadoFuncionarioPublico.getSegundoApellido());
                datosUsuario.setCargo(certificadoFuncionarioPublico.getCargo());
                datosUsuario.setInstitucion(certificadoFuncionarioPublico.getInstitucion());
            }
            if (certificadoSecurityData instanceof CertificadoPersonaJuridica certificadoPersonaJuridica) {
                datosUsuario.setCedula(certificadoPersonaJuridica.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaJuridica.getNombres());
                datosUsuario.setApellido(certificadoPersonaJuridica.getPrimerApellido() + " "
                        + certificadoPersonaJuridica.getSegundoApellido());
                datosUsuario.setCargo(certificadoPersonaJuridica.getCargo());
            }
            if (certificadoSecurityData instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido() + " "
                        + certificadoPersonaNatural.getSegundoApellido());
            }
            if (certificadoSecurityData instanceof CertificadoSelladoTiempo) {
                datosUsuario.setCertificadoDigitalValido(true);
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoAnfAc18332Factory.esCertificadoDeAnfAc18332(certificado)) {
            CertificadoAnfAc18332 certificadoAnfAc18332 = CertificadoAnfAc18332Factory.construir(certificado);
            if (certificadoAnfAc18332 instanceof CertificadoFuncionarioPublico certificadoFuncionarioPublico) {
                datosUsuario.setCedula(certificadoFuncionarioPublico.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoFuncionarioPublico.getNombres());
                datosUsuario.setApellido(certificadoFuncionarioPublico.getPrimerApellido() + " "
                        + certificadoFuncionarioPublico.getSegundoApellido());
                datosUsuario.setCargo(certificadoFuncionarioPublico.getCargo());
                datosUsuario.setInstitucion(certificadoFuncionarioPublico.getInstitucion());
            }
            if (certificadoAnfAc18332 instanceof CertificadoPersonaJuridica certificadoPersonaJuridica) {
                datosUsuario.setCedula(certificadoPersonaJuridica.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaJuridica.getNombres());
                datosUsuario.setApellido(certificadoPersonaJuridica.getPrimerApellido() + " "
                        + certificadoPersonaJuridica.getSegundoApellido());
                datosUsuario.setCargo(certificadoPersonaJuridica.getCargo());
            }
            if (certificadoAnfAc18332 instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido() + " "
                        + certificadoPersonaNatural.getSegundoApellido());
            }
            if (certificadoAnfAc18332 instanceof CertificadoSelladoTiempo) {
                datosUsuario.setCertificadoDigitalValido(true);
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoAnfAc37442Factory.esCertificadoDeAnfAc37442(certificado)) {
            CertificadoAnfAc37442 certificadoAnfAc37442 = CertificadoAnfAc37442Factory.construir(certificado);
            if (certificadoAnfAc37442 instanceof CertificadoFuncionarioPublico certificadoFuncionarioPublico) {
                datosUsuario.setCedula(certificadoFuncionarioPublico.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoFuncionarioPublico.getNombres());
                datosUsuario.setApellido(certificadoFuncionarioPublico.getPrimerApellido() + " "
                        + certificadoFuncionarioPublico.getSegundoApellido());
                datosUsuario.setCargo(certificadoFuncionarioPublico.getCargo());
                datosUsuario.setInstitucion(certificadoFuncionarioPublico.getInstitucion());
            }
            if (certificadoAnfAc37442 instanceof CertificadoPersonaJuridica certificadoPersonaJuridica) {
                datosUsuario.setCedula(certificadoPersonaJuridica.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaJuridica.getNombres());
                datosUsuario.setApellido(certificadoPersonaJuridica.getPrimerApellido() + " "
                        + certificadoPersonaJuridica.getSegundoApellido());
                datosUsuario.setCargo(certificadoPersonaJuridica.getCargo());
            }
            if (certificadoAnfAc37442 instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido() + " "
                        + certificadoPersonaNatural.getSegundoApellido());
            }
            if (certificadoAnfAc37442 instanceof CertificadoSelladoTiempo) {
                datosUsuario.setCertificadoDigitalValido(true);
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoDigercicFactory.esCertificadoDigercic(certificado)) {
            CertificadoDigercic certificadoDigercic = CertificadoDigercicFactory.construir(certificado);
            if (certificadoDigercic instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                datosUsuario.setNombre(Utils.getCN(certificado));
                datosUsuario.setApellido("");
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoUanatacaDataFactory.esCertificadoUanataca(certificado)) {
            CertificadoUanataca certificadoUanataca = CertificadoUanatacaDataFactory.construir(certificado);
            if (certificadoUanataca instanceof CertificadoMiembroEmpresaUanataca certificadoMiembroEmpresaUanataca) {
                datosUsuario.setCedula(certificadoMiembroEmpresaUanataca.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoMiembroEmpresaUanataca.getNombres());
                datosUsuario.setApellido(certificadoMiembroEmpresaUanataca.getPrimerApellido() + " "
                        + certificadoMiembroEmpresaUanataca.getSegundoApellido());
                datosUsuario.setCargo(certificadoMiembroEmpresaUanataca.getCargo());
            }
            if (certificadoUanataca instanceof CertificadoPersonaJuridicaPrivadaUanataca certificadoPersonaJuridicaUanataca) {
                datosUsuario.setCedula(certificadoPersonaJuridicaUanataca.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaJuridicaUanataca.getNombres());
                datosUsuario.setApellido(certificadoPersonaJuridicaUanataca.getPrimerApellido() + " "
                        + certificadoPersonaJuridicaUanataca.getSegundoApellido());
                datosUsuario.setCargo(datosUsuario.getCargo());
            }
            if (certificadoUanataca instanceof CertificadoPersonaNaturalUanataca certificadoPersonaNaturalU) {
                datosUsuario.setCedula(certificadoPersonaNaturalU.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaNaturalU.getNombres());
                datosUsuario.setApellido(certificadoPersonaNaturalU.getPrimerApellido() + " "
                        + certificadoPersonaNaturalU.getSegundoApellido());
            }
            if (certificadoUanataca instanceof CertificadoRepresentanteLegalUanataca certificadoRepresentanteLegalUanataca) {
                datosUsuario.setCedula(certificadoRepresentanteLegalUanataca.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoRepresentanteLegalUanataca.getNombres());
                datosUsuario.setApellido(certificadoRepresentanteLegalUanataca.getPrimerApellido() + " "
                        + certificadoRepresentanteLegalUanataca.getSegundoApellido());
                datosUsuario.setCargo(certificadoRepresentanteLegalUanataca.getCargo());
            }
            if (certificadoUanataca instanceof CertificadoSelladoTiempo) {
                datosUsuario.setCertificadoDigitalValido(true);
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoEclipsoftDataFactory.esCertificadoEclipsoft(certificado)) {
            CertificadoEclipsoft certificadoEclipsoft = CertificadoEclipsoftDataFactory.construir(certificado);
            if (certificadoEclipsoft instanceof CertificadoPersonalNaturalEclipsoft certificadoPersonalNaturalEclipsoft) {
                datosUsuario.setCedula(certificadoPersonalNaturalEclipsoft.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonalNaturalEclipsoft.getNombres());
                datosUsuario.setApellido(certificadoPersonalNaturalEclipsoft.getPrimerApellido() + " " + certificadoPersonalNaturalEclipsoft.getSegundoApellido());
            }
            if (certificadoEclipsoft instanceof CertificadoMiembroEmpresaEclipsoft certificadoMiembroEmpresaEclipsoft) {
                datosUsuario.setCedula(certificadoMiembroEmpresaEclipsoft.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoMiembroEmpresaEclipsoft.getNombres());
                datosUsuario.setApellido(certificadoMiembroEmpresaEclipsoft.getPrimerApellido() + " " + certificadoMiembroEmpresaEclipsoft.getSegundoApellido());
                datosUsuario.setCargo(certificadoMiembroEmpresaEclipsoft.getCargo());
            }
            if (certificadoEclipsoft instanceof CertificadoRepresentanteLegalEclipsoft certificadoRepresentanteLegalEclipsoft) {
                datosUsuario.setCedula(certificadoRepresentanteLegalEclipsoft.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoRepresentanteLegalEclipsoft.getNombres());
                datosUsuario.setApellido(certificadoRepresentanteLegalEclipsoft.getPrimerApellido() + " " + certificadoRepresentanteLegalEclipsoft.getSegundoApellido());
                datosUsuario.setCargo(certificadoRepresentanteLegalEclipsoft.getCargo());
            }
            if (certificadoEclipsoft instanceof CertificadoPersonaJuridicaPrivadaEclipsoft certificadoPersonaJuridicaPrivadaEclipsoft) {
                datosUsuario.setCedula(certificadoPersonaJuridicaPrivadaEclipsoft.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaJuridicaPrivadaEclipsoft.getNombres());
                datosUsuario.setApellido(certificadoPersonaJuridicaPrivadaEclipsoft.getPrimerApellido() + " " + certificadoPersonaJuridicaPrivadaEclipsoft.getSegundoApellido());
                datosUsuario.setCargo(datosUsuario.getCargo());
            }
            if (certificadoEclipsoft instanceof CertificadoSelladoTiempo) {
                datosUsuario.setCertificadoDigitalValido(true);
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoDatilDataFactory.esCertificadoDatil(certificado)) {
            CertificadoDatil certificadoDatil = CertificadoDatilDataFactory.construir(certificado);
            if (certificadoDatil instanceof CertificadoMiembroEmpresaDatil certificadoMiembroEmpresaDatil) {
                datosUsuario.setCedula(certificadoMiembroEmpresaDatil.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoMiembroEmpresaDatil.getNombres());
                datosUsuario.setApellido(certificadoMiembroEmpresaDatil.getPrimerApellido() + " " + certificadoMiembroEmpresaDatil.getSegundoApellido());
                datosUsuario.setCargo(certificadoMiembroEmpresaDatil.getCargo());
            }
            if (certificadoDatil instanceof CertificadoPersonaJuridicaPrivadaDatil certificadoPersonaJuridicaPrivadaDatil) {
                datosUsuario.setCedula(certificadoPersonaJuridicaPrivadaDatil.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaJuridicaPrivadaDatil.getNombres());
                datosUsuario.setApellido(certificadoPersonaJuridicaPrivadaDatil.getPrimerApellido() + " "
                        + certificadoPersonaJuridicaPrivadaDatil.getSegundoApellido());
                datosUsuario.setCargo(certificadoPersonaJuridicaPrivadaDatil.getCargo());
            }
            if (certificadoDatil instanceof CertificadoRepresentanteLegalDatil certificadoRepresentanteLegalDatil) {
                datosUsuario.setCedula(certificadoRepresentanteLegalDatil.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoRepresentanteLegalDatil.getNombres());
                datosUsuario.setApellido(certificadoRepresentanteLegalDatil.getPrimerApellido() + " "
                        + certificadoRepresentanteLegalDatil.getSegundoApellido());
                datosUsuario.setCargo(certificadoRepresentanteLegalDatil.getCargo());
            }
            if (certificadoDatil instanceof CertificadoPersonaNaturalDatil certificadoPersonaNaturalDatil) {
                datosUsuario.setCedula(certificadoPersonaNaturalDatil.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaNaturalDatil.getNombres());
                datosUsuario.setApellido(certificadoPersonaNaturalDatil.getPrimerApellido() + " "
                        + certificadoPersonaNaturalDatil.getSegundoApellido());
            }
            if (certificadoDatil instanceof CertificadoSelladoTiempo) {
                datosUsuario.setCertificadoDigitalValido(true);
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoArgosDataFactory.esCertificadoArgosData(certificado)) {
            CertificadoArgosData certificadoArgosData = CertificadoArgosDataFactory.construir(certificado);
            if (certificadoArgosData instanceof CertificadoPersonaNaturalArgosData certificadoPersonaNatural) {
                datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido() + " "
                        + certificadoPersonaNatural.getSegundoApellido());
            }
            if (certificadoArgosData instanceof CertificadoRepresentanteLegalArgosData certificadoRepresentanteLegal) {
                datosUsuario.setCedula(certificadoRepresentanteLegal.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoRepresentanteLegal.getNombres());
                datosUsuario.setApellido(certificadoRepresentanteLegal.getPrimerApellido() + " "
                        + certificadoRepresentanteLegal.getSegundoApellido());
                datosUsuario.setCargo(certificadoRepresentanteLegal.getCargo());
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoLazzateDataFactory.esCertificadoLazzate(certificado)) {
            CertificadoLazzate certificadoLazzate = CertificadoLazzateDataFactory.construir(certificado);
            if (certificadoLazzate instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                datosUsuario.setTipoCertificado("Persona Natural (EXT)");
                if (certificadoPersonaNatural.getNombres().isEmpty()) {
                    datosUsuario.setNombre(Utils.getCN(certificado));
                    datosUsuario.setApellido("");
                } else {
                    datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                    datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido() + " "
                            + certificadoPersonaNatural.getSegundoApellido());
                }
            }
            if (certificadoLazzate instanceof CertificadoPersonaJuridica certificadoPersonaJuridica) {
                datosUsuario.setCedula(certificadoPersonaJuridica.getCedulaPasaporte());
                datosUsuario.setTipoCertificado("Persona Juridica (EXT)");
                datosUsuario.setInstitucion(certificadoPersonaJuridica.getRazonSocial());
                datosUsuario.setCargo(certificadoPersonaJuridica.getCargo());
                if (certificadoPersonaJuridica.getNombres().isEmpty()) {
                    datosUsuario.setNombre(Utils.getCN(certificado));
                    datosUsuario.setApellido("");
                } else {
                    datosUsuario.setNombre(certificadoPersonaJuridica.getNombres());
                    datosUsuario.setApellido(certificadoPersonaJuridica.getPrimerApellido() + " "
                            + certificadoPersonaJuridica.getSegundoApellido());
                }
            }
            // Fallback: si cedula vacia, extraer todo de Subject DN
            if (datosUsuario.getCedula() == null || datosUsuario.getCedula().isEmpty()) {
                try {
                    org.bouncycastle.asn1.x500.X500Name subjectName = new org.bouncycastle.cert.jcajce.JcaX509CertificateHolder(certificado).getSubject();
                    // Cedula
                    String serialNumber = CertUtils.getSubjectFieldByOID(subjectName, org.bouncycastle.asn1.x500.style.BCStyle.SERIALNUMBER);
                    if (serialNumber != null && !serialNumber.isEmpty()) {
                        datosUsuario.setCedula(serialNumber);
                    }
                    // Nombre y apellido
                    String givenName = CertUtils.getSubjectFieldByOID(subjectName, org.bouncycastle.asn1.x500.style.BCStyle.GIVENNAME);
                    String surname = CertUtils.getSubjectFieldByOID(subjectName, org.bouncycastle.asn1.x500.style.BCStyle.SURNAME);
                    if (givenName != null && !givenName.isEmpty()) {
                        datosUsuario.setNombre(givenName);
                        datosUsuario.setApellido(surname != null ? surname : "");
                    }
                    // Institucion (Organization)
                    if (datosUsuario.getInstitucion() == null || datosUsuario.getInstitucion().isEmpty()) {
                        String organization = CertUtils.getSubjectFieldByOID(subjectName, org.bouncycastle.asn1.x500.style.BCStyle.O);
                        if (organization != null && !organization.isEmpty()) {
                            datosUsuario.setInstitucion(organization);
                        }
                    }
                    // Cargo (Title)
                    if (datosUsuario.getCargo() == null || datosUsuario.getCargo().isEmpty()) {
                        String title = CertUtils.getSubjectFieldByOID(subjectName, org.bouncycastle.asn1.x500.style.BCStyle.T);
                        if (title != null && !title.isEmpty()) {
                            datosUsuario.setCargo(title);
                        }
                    }
                    // Unidad organizacional (OU) como cargo alternativo
                    if (datosUsuario.getCargo() == null || datosUsuario.getCargo().isEmpty()) {
                        String ou = CertUtils.getSubjectFieldByOID(subjectName, org.bouncycastle.asn1.x500.style.BCStyle.OU);
                        if (ou != null && !ou.isEmpty()) {
                            datosUsuario.setCargo(ou);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error extrayendo datos de Subject DN para Lazzate: {0}", e.getMessage());
                }
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoAlphaTechnologiesFactory.esCertificadoDeAlphaTechnologies(certificado)) {
            Certificado certificadoAlphaTechnologies = CertificadoAlphaTechnologiesFactory.construir(certificado);
            if (certificadoAlphaTechnologies instanceof CertificadoAlphaTechnologiesImpl) {
                if (certificadoAlphaTechnologies instanceof CertificadoMiembroEmpresa certificadoMiembroEmpresa) {
                    datosUsuario.setCedula(certificadoMiembroEmpresa.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoMiembroEmpresa.getNombres());
                    datosUsuario.setApellido(certificadoMiembroEmpresa.getPrimerApellido() + " "
                            + certificadoMiembroEmpresa.getSegundoApellido());
                    datosUsuario.setCargo(certificadoMiembroEmpresa.getCargo());
                }
                if (certificadoAlphaTechnologies instanceof CertificadoPersonaJuridica certificadoPersonaJuridica) {
                    datosUsuario.setCedula(certificadoPersonaJuridica.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoPersonaJuridica.getNombres());
                    datosUsuario.setApellido(certificadoPersonaJuridica.getPrimerApellido() + " "
                            + certificadoPersonaJuridica.getSegundoApellido());
                    datosUsuario.setCargo(certificadoPersonaJuridica.getCargo());
                }
                if (certificadoAlphaTechnologies instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                    datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                    datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido() + " "
                            + certificadoPersonaNatural.getSegundoApellido());
                }
            }
            //RESOLUCION-ARCOTEL-2024-0176
            if (certificadoAlphaTechnologies instanceof CertificadoSubjAlphaTechnologiesImpl) {
                if (certificadoAlphaTechnologies instanceof CertificadoMiembroEmpresa certificadoMiembroEmpresa) {
                    datosUsuario.setCedula(certificadoMiembroEmpresa.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoMiembroEmpresa.getNombres());
                    datosUsuario.setApellido(certificadoMiembroEmpresa.getPrimerApellido());
                    datosUsuario.setCargo(certificadoMiembroEmpresa.getCargo());
                }
                if (certificadoAlphaTechnologies instanceof CertificadoRepresentanteLegal certificadoRepresentanteLegal) {
                    datosUsuario.setCedula(certificadoRepresentanteLegal.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoRepresentanteLegal.getNombres());
                    datosUsuario.setApellido(certificadoRepresentanteLegal.getPrimerApellido());
                    datosUsuario.setCargo(certificadoRepresentanteLegal.getCargo());
                }
                if (certificadoAlphaTechnologies instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                    datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                    datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido());
                }
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoCorpNewBestDataFactory.esCertificadoCorpNewBest(certificado)) {
            CertificadoCorpNewBest certificadoCorpNewBest = CertificadoCorpNewBestDataFactory.construir(certificado);
            if (certificadoCorpNewBest instanceof CertificadoPersonaJuridica certificadoPersonaJuridica) {
                datosUsuario.setCedula(certificadoPersonaJuridica.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaJuridica.getNombres());
                datosUsuario.setApellido(certificadoPersonaJuridica.getPrimerApellido() + " "
                        + certificadoPersonaJuridica.getSegundoApellido());
                datosUsuario.setCargo(certificadoPersonaJuridica.getCargo());
            }
            if (certificadoCorpNewBest instanceof CertificadoMiembroEmpresa certificadoMiembroEmpresa) {
                datosUsuario.setCedula(certificadoMiembroEmpresa.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoMiembroEmpresa.getNombres());
                datosUsuario.setApellido(certificadoMiembroEmpresa.getPrimerApellido() + " "
                        + certificadoMiembroEmpresa.getSegundoApellido());
                datosUsuario.setCargo(certificadoMiembroEmpresa.getCargo());
            }
            if (certificadoCorpNewBest instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido() + " "
                        + certificadoPersonaNatural.getSegundoApellido());
            }
            if (certificadoCorpNewBest instanceof CertificadoSelladoTiempo) {
                datosUsuario.setCertificadoDigitalValido(true);
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        if (CertificadoFirmaSeguraFactory.esCertificadoDeFirmaSegura(certificado)) {
            CertificadoFirmaSegura certificadoFirmaSegura = CertificadoFirmaSeguraFactory.construir(certificado);
            if (certificadoFirmaSegura instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido() + " "
                        + certificadoPersonaNatural.getSegundoApellido());
            }
            if (certificadoFirmaSegura instanceof CertificadoRepresentanteLegalFirmaSegura certificadoRepresentanteLegal) {
                datosUsuario.setCedula(certificadoRepresentanteLegal.getCedulaPasaporte());
                datosUsuario.setNombre(certificadoRepresentanteLegal.getNombres());
                datosUsuario.setApellido(certificadoRepresentanteLegal.getPrimerApellido() + " "
                        + certificadoRepresentanteLegal.getSegundoApellido());
                datosUsuario.setCargo(certificadoRepresentanteLegal.getCargo());
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        //RESOLUCION-ARCOTEL-2024-0176
        if (CertificadoLetmiFactory.esCertificadoDeLetmi(certificado)) {
            Certificado certificadoLetmi = CertificadoLetmiFactory.construir(certificado);
            if (certificadoLetmi instanceof CertificadoSubjLetmiImpl) {
                if (certificadoLetmi instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                    datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                    datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido());
                }
                if (certificadoLetmi instanceof CertificadoMiembroEmpresa certificadoMiembroEmpresa) {
                    datosUsuario.setCedula(certificadoMiembroEmpresa.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoMiembroEmpresa.getNombres());
                    datosUsuario.setApellido(certificadoMiembroEmpresa.getPrimerApellido());
                    datosUsuario.setCargo(certificadoMiembroEmpresa.getCargo());
                }
                if (certificadoLetmi instanceof CertificadoRepresentanteLegal certificadoRepresentanteLegal) {
                    datosUsuario.setCedula(certificadoRepresentanteLegal.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoRepresentanteLegal.getNombres());
                    datosUsuario.setApellido(certificadoRepresentanteLegal.getPrimerApellido());
                    datosUsuario.setCargo(certificadoRepresentanteLegal.getCargo());
                }
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        //RESOLUCION-ARCOTEL-2024-0176
        if (CertificadoAppFirmasFactory.esCertificadoDeAppFirmas(certificado)) {
            Certificado certificadoAppFirmas = CertificadoAppFirmasFactory.construir(certificado);
            if (certificadoAppFirmas instanceof CertificadoSubjAppFirmasImpl) {
                if (certificadoAppFirmas instanceof CertificadoPersonaNatural certificadoPersonaNatural) {
                    datosUsuario.setCedula(certificadoPersonaNatural.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoPersonaNatural.getNombres());
                    datosUsuario.setApellido(certificadoPersonaNatural.getPrimerApellido());
                }
                if (certificadoAppFirmas instanceof CertificadoMiembroEmpresa certificadoMiembroEmpresa) {
                    datosUsuario.setCedula(certificadoMiembroEmpresa.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoMiembroEmpresa.getNombres());
                    datosUsuario.setApellido(certificadoMiembroEmpresa.getPrimerApellido());
                    datosUsuario.setCargo(certificadoMiembroEmpresa.getCargo());
                }
                if (certificadoAppFirmas instanceof CertificadoRepresentanteLegal certificadoRepresentanteLegal) {
                    datosUsuario.setCedula(certificadoRepresentanteLegal.getCedulaPasaporte());
                    datosUsuario.setNombre(certificadoRepresentanteLegal.getNombres());
                    datosUsuario.setApellido(certificadoRepresentanteLegal.getPrimerApellido());
                    datosUsuario.setCargo(certificadoRepresentanteLegal.getCargo());
                }
            }
            datosUsuario.setCertificadoDigitalValido(true);
            return datosUsuario;
        }

        return null;
    }
}
