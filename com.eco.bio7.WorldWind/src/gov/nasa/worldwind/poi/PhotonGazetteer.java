package gov.nasa.worldwind.poi;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.exception.NoItemException;
import gov.nasa.worldwind.exception.ServiceException;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.formats.geojson.GeoJSONDoc;
import gov.nasa.worldwind.formats.geojson.GeoJSONFeature;
import gov.nasa.worldwind.formats.geojson.GeoJSONFeatureCollection;
import gov.nasa.worldwind.formats.geojson.GeoJSONGeometry;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwindx.examples.GeoJSONLoader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * A gazetteer that uses Photon geocoding service to find locations for requested places.
 *
 * @author jyc
 * Source from: https://forum.worldwindcentral.com/forum/world-wind-java-forums/development-help/13553-another-geocoder-software-instead-of-yahoogazetteer/page2
 */
public class PhotonGazetteer implements Gazetteer {
    protected static final String GEOCODE_SERVICE = "https://photon.komoot.de/api/?q=";

    public List<PointOfInterest> findPlaces(String lookupString) throws NoItemException, ServiceException {
        if (lookupString == null || lookupString.length() < 1)
            return null;

        String urlString = buildUrlString(lookupString);
        GeoJSONLoader geojson = new GeoJSONLoader();

        return this.parseLocationString(geojson, urlString);
    }

    private String buildUrlString(String lookupString) {

        String urlString;

        try {
            urlString = GEOCODE_SERVICE + URLEncoder.encode(lookupString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            urlString = GEOCODE_SERVICE + lookupString.replaceAll(" ", "+");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return urlString;
    }

    protected List<PointOfInterest> parseLocationString(GeoJSONLoader geojson, Object docSource) throws WWRuntimeException {

        if (WWUtil.isEmpty(docSource)) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        List<PointOfInterest> positions = new ArrayList<PointOfInterest>();

        GeoJSONDoc doc = null;
        try {
            doc = new GeoJSONDoc(docSource);
            doc.parse();

            if (doc.getRootObject() instanceof GeoJSONFeatureCollection) {
                GeoJSONFeatureCollection collec = (GeoJSONFeatureCollection) doc.getRootObject();

                GeoJSONFeature[] features = collec.getFeatures();

                for (GeoJSONFeature feature : features) {
                    GeoJSONGeometry geometry = feature.getGeometry();
                    String name = (String) feature.getProperties().getValue("name");
                    String stateName = (String) feature.getProperties().getValue("state");
                    String countryName = (String) feature.getProperties().getValue("country");
                    Position position = geometry.asPoint().getPosition();
                    LatLon latlon = LatLon.fromDegrees(position.getLatitude().degrees, position.getLongitude().degrees);
                    PointOfInterest loc = new BasicPointOfInterest(latlon);
                    loc.setValue(AVKey.DISPLAY_NAME, name + " - " + stateName + " - " + countryName);
                    System.out.println(loc);
                    positions.add(loc);
                }
            } else {
                geojson.handleUnrecognizedObject(doc.getRootObject());
            }
        } catch (IOException e) {
            String message = Logging.getMessage("generic.ExceptionAttemptingToReadGeoJSON", docSource);
            Logging.logger().log(Level.SEVERE, message, e);
            throw new WWRuntimeException(message, e);
        } finally {
            WWIO.closeStream(doc, docSource.toString());
        }
       

        return positions;
    }

}