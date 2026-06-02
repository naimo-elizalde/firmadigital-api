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

/**
 * Datos del usuario para contruir la validacion CMS.
 *
 * @author Ricardo Arguello
 */
public class DatosUsuario {

    private String cedula;
    private String nombre;
    private String apellido;
    private String institucion = "";
    private String cargo = "";
    private String tipoCertificado = "";
    private boolean certificadoDigitalValido;
    private String fechaFirmaArchivoP7M;

    public DatosUsuario() {
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = obtenerCedula(cedula);
    }

    private String obtenerCedula(String cedula) {
        if (cedula == null) {
            return null;
        }
        return cedula.replace("IDCEC-", "")
                .replace("PASEC-", "");
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getInstitucion() {
        return institucion;
    }

    public void setInstitucion(String institucion) {
        this.institucion = institucion;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getTipoCertificado() {
        return tipoCertificado;
    }

    public void setTipoCertificado(String tipoCertificado) {
        this.tipoCertificado = tipoCertificado;
    }

    public boolean isCertificadoDigitalValido() {
        return certificadoDigitalValido;
    }

    public void setCertificadoDigitalValido(boolean certificadoDigitalValido) {
        this.certificadoDigitalValido = certificadoDigitalValido;
    }

    public void setFechaFirmaArchivoP7M(String fechaFirmaArchivoP7M) {
        this.fechaFirmaArchivoP7M = fechaFirmaArchivoP7M;
    }

    @Override
    public String toString() {
        return """
               DatosUsuario
               \t\t[cedula=""" + cedula + "\n"
                + "\t\tnombre=" + nombre + "\n"
                + "\t\tapellido=" + apellido + "\n"
                + "\t\tinstitucion=" + institucion + "\n"
                + "\t\tcargo=" + cargo + "\n"
                + "\t\tcertificadoDigitalValido=" + certificadoDigitalValido + "\n"
                + "\t\tfechaFirmaArchivoP7M=" + fechaFirmaArchivoP7M + "\n"
                + "\t\t]";
    }
}
