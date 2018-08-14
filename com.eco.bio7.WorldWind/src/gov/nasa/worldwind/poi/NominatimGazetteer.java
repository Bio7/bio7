/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.poi;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.exception.ServiceException;
import gov.nasa.worldwind.exception.NoItemException;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.util.Logging;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * A gazetteer that uses Nominatim's geocoding service to find locations for requested places.
 *
 * @author tag
 * @version $Id: NominatimGazetteer.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class NominatimGazetteer implements Gazetteer
{
    protected static final String GEOCODE_SERVICE = "http://nominatim.openstreetmap.org/search?format=xml&q=";

    public List<PointOfInterest> findPlaces(String lookupString) throws NoItemException, ServiceException
    {
        if (lookupString == null || lookupString.length() < 1)
            return null;

        String urlString;

        try
        {
            urlString = GEOCODE_SERVICE + URLEncoder.encode(lookupString, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            urlString = GEOCODE_SERVICE + lookupString.replaceAll(" ", "+");
        }
        catch( Exception e )
        {
            e.printStackTrace();
            return null ;
        }

        System.out.println( "urlString=" + urlString );

        String locationString = POIUtils.callService(urlString);
        
        if (locationString == null || locationString.length() < 1)
            return null;

        return this.parseLocationString(locationString);
    }

    protected ArrayList<PointOfInterest> parseLocationString(String locationString) throws WWRuntimeException
    {
        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(false);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new ByteArrayInputStream(locationString.getBytes("UTF-8")));

            XPathFactory xpFactory = XPathFactory.newInstance();
            XPath xpath = xpFactory.newXPath();

            NodeList resultNodes =
                //(NodeList) xpath.evaluate("/ResultSet/Result", doc, XPathConstants.NODESET);
                (NodeList) xpath.evaluate("/searchresults/place", doc, XPathConstants.NODESET);

            ArrayList<PointOfInterest> positions = new ArrayList<PointOfInterest>(resultNodes.getLength());

            for (int i = 0; i < resultNodes.getLength(); i++)
            {
                Node node = resultNodes.item(i);

                System.out.println( "node=" + node.toString() );

                String lat = "" ;
                String lon = "" ;
                String pla = "" ;

                NamedNodeMap nj_atts = node.getAttributes();

                for (int j = 0; j < nj_atts.getLength(); j++)
                {
                    Node nj_att = node.getAttributes().item(j);

                    String att_name = nj_att.getNodeName();

                    if ( att_name.equals("lat") )
                        lat = nj_att.getNodeValue() ;

                    if ( att_name.equals("lon") )
                        lon = nj_att.getNodeValue() ;

                    if ( att_name.equals("display_name") )
                        pla = nj_att.getNodeValue() ;
                }

                if ( lat.equals("") == false && lon.equals("") == false && pla.equals("") == false )
                {
                    LatLon latlon = LatLon.fromDegrees(Double.parseDouble(lat), Double.parseDouble(lon));
                    PointOfInterest loc = new BasicPointOfInterest(latlon);
                    loc.setValue(AVKey.DISPLAY_NAME, pla );
                    positions.add(loc);
                }
            }

            return positions;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null ;
        }
    }
}  