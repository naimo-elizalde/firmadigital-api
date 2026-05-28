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
package ec.gob.firmadigital.libreria.certificate.to;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Objeto para acceder informacion legible del certificado digital
 *
 * @author Misael Fernández
 */
public class Certificado {

    private String serial;//Serial del certificado digital
    private String issuedTo;//Información del firmante
    private String issuedBy;//Información de la entidad certificadora
    private Calendar validFrom;//certificado digital válido desde
    private Calendar validTo;//certificado digital válido hasta
    private Calendar revocated;//fecha de revocado del certificado digital
    private String keyUsages;//llaves de uso
    private Boolean certificateValidated;//validación del certificado en las fecha de vigencia
    private Calendar signGenerated;//fecha de firmar del documento
    private Boolean signVerify;//Integridad Firma
    private String docReason;//Razón del documento
    private String docLocation;//Localización del documento
    private Boolean docValidTimeStamp;//Estampa de tiempo
    private String docTimeStampIssuedBy;//Información de la entidad certificadora (Estampa de tiempo)
    private Date docTimeStamp;//Estampa de tiempo
    private String cnTimeStamp;//CN del certificado TSA
    private DatosUsuario datosUsuario;

    public Certificado() {
    }

    public Certificado(String serial, String issuedTo, String issuedBy, Calendar validFrom, Calendar validTo, Calendar signGenerated, Calendar revocated, Boolean certificateValidated, DatosUsuario datosUsuario) {
        this.serial = serial;
        this.issuedTo = issuedTo;
        this.issuedBy = issuedBy;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.signGenerated = signGenerated;
        this.revocated = revocated;
        this.certificateValidated = certificateValidated;
        this.datosUsuario = datosUsuario;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getIssuedTo() {
        return issuedTo;
    }

    public void setIssuedTo(String issuedTo) {
        this.issuedTo = issuedTo;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public Calendar getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Calendar validFrom) {
        this.validFrom = validFrom;
    }

    public Calendar getValidTo() {
        return validTo;
    }

    public void setValidTo(Calendar validTo) {
        this.validTo = validTo;
    }

    public Calendar getRevocated() {
        return revocated;
    }

    public void setRevocated(Calendar revocated) {
        this.revocated = revocated;
    }

    public String getKeyUsages() {
        return keyUsages;
    }

    public void setKeyUsages(String keyUsages) {
        this.keyUsages = keyUsages;
    }

    public Boolean getCertificateValidated() {
        return certificateValidated;
    }

    public void setCertificateValidated(Boolean validated) {
        this.certificateValidated = validated;
    }

    public Boolean getSignVerify() {
        return signVerify;
    }

    public void setSignVerify(Boolean signVerify) {
        this.signVerify = signVerify;
    }

    public Calendar getSignGenerated() {
        return signGenerated;
    }

    public void setSignGenerated(Calendar signGenerated) {
        this.signGenerated = signGenerated;
    }

    public String getDocReason() {
        return docReason;
    }

    public void setDocReason(String docReason) {
        this.docReason = docReason;
    }

    public String getDocLocation() {
        return docLocation;
    }

    public void setDocLocation(String docLocation) {
        this.docLocation = docLocation;
    }

    public Boolean getDocValidTimeStamp() {
        return docValidTimeStamp;
    }

    public void setDocValidTimeStamp(Boolean docValidTimeStamp) {
        this.docValidTimeStamp = docValidTimeStamp;
    }

    public String getDocTimeStampIssuedBy() {
        return docTimeStampIssuedBy;
    }

    public void setDocTimeStampIssuedBy(String docTimeStampIssuedBy) {
        this.docTimeStampIssuedBy = docTimeStampIssuedBy;
    }

    public Date getDocTimeStamp() {
        return docTimeStamp;
    }

    public void setDocTimeStamp(Date docTimeStamp) {
        this.docTimeStamp = docTimeStamp;
    }

    public String getCnTimeStamp() {
        return cnTimeStamp;
    }

    public void setCnTimeStamp(String cnTimeStamp) {
        this.cnTimeStamp = cnTimeStamp;
    }

    public DatosUsuario getDatosUsuario() {
        return datosUsuario;
    }

    public void setDatosUsuario(DatosUsuario datosUsuario) {
        this.datosUsuario = datosUsuario;
    }

    @Override
    public String toString() {
        return "\tCertificado\n"
                + "\t[serial=" + serial + "\n"
                + "\tissuedTo=" + issuedTo + "\n"
                + "\tissuedBy=" + issuedBy + "\n"
                + "\tvalidFrom=" + (validFrom == null ? null : (String) new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((validFrom.getTime()))) + "\n"
                + "\tvalidTo=" + (validTo == null ? null : (String) new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((validTo.getTime()))) + "\n"
                + "\trevocated=" + (revocated == null ? null : (String) new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((revocated.getTime()))) + "\n"
                + "\tkeyUsages=" + keyUsages + "\n"
                + "\tcertificateValidated=" + certificateValidated + "\n"
                + "\tsignGenerated=" + (signGenerated == null ? null : (String) new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((signGenerated.getTime()))) + "\n"
                + "\tsignVerify=" + signVerify + "\n"
                + "\tdocReason=" + docReason + "\n"
                + "\tdocLocation=" + docLocation + "\n"
                + "\tdocValidTimeStamp=" + docValidTimeStamp + "\n"
                + "\tdocTimeStampIssuedBy=" + docTimeStampIssuedBy + "\n"
                + "\tdocTimeStamp=" + (docTimeStamp == null ? null : (String) new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((docTimeStamp))) + "\n"
                + "\tcnTimeStamp=" + cnTimeStamp + "\n"
                + "\t" + (datosUsuario == null ? "DatosUsuario[Sin información de usuario]" : datosUsuario.toString()) + "\n"
                + "\t]"
                + "\n----------";
    }

}
