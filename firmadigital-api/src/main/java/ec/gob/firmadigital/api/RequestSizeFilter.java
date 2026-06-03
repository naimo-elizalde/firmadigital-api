/*
 * Firma Digital: API
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ec.gob.firmadigital.api;

/**
 *
 * @author Misael Fernandez
 */
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class RequestSizeFilter implements ContainerRequestFilter {

    /**
     * Application Configuracion using System Properties.
     *
     * Se debe almacenar en el archivo de configuracion del servidor WildFly
     * (standalone.xml):
     *
     * <property name="firmadigital-api-mobile.appverificardocumento.request.size" value= "XX" />
     * <property name="firmadigital-api-mobile.appvalidarcertificadodigital.request.size" value= "XX" />
     * <property name="firmadigital-api-mobile.appfirmardocumento.request.size" value= "XX" />
     * <property name="firmadigital-api-mobile.appfirmardocumentotransversal.request.size" value= "XX" />
     * <property name="firmadigital-api.request.size" value= "XX" />
     *
     * Nombre de las propiedades que contiene el servicio web (valor en KB)
     */

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String propertyName = switch (requestContext.getUriInfo().getMatchedURIs().get(0)) {
            case "appverificardocumento" -> "firmadigital-api-mobile.appverificardocumento.request.size";
            case "appvalidarcertificadodigital" -> "firmadigital-api-mobile.appvalidarcertificadodigital.request.size";
            case "appfirmardocumento" -> "firmadigital-api-mobile.appfirmardocumento.request.size";
            case "appfirmardocumentoconqr" -> "firmadigital-api-mobile.appfirmardocumento.request.size";
            case "appfirmardocumentotransversal" -> "firmadigital-api-mobile.appfirmardocumentotransversal.request.size";
            default -> "firmadigital-api.request.size";
        };
        String sizeStr = System.getProperty(propertyName);
        int maxRequestSize = sizeStr != null ? Integer.parseInt(sizeStr) : 512000;//KB
        long contentLength = requestContext.getLength();
        if (requestContext.getMethod().equals("POST")
                && contentLength > (maxRequestSize * 1024L)) {//KB to BYTE
            requestContext.abortWith(Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
                    .entity("La solicitud no puede exceder los " + maxRequestSize / 1024 + " MB")//KB to MB
                    .build());
        }
    }
}
