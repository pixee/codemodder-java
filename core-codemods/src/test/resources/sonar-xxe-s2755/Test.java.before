package com.acme;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Holds various XXE vulns for different APIs. */
public class XXEVuln {

    public static void main(String[] args) throws TransformerException, ParserConfigurationException, IOException, SAXException, SQLException {
        docToString(null);
        saxTransformer(args[0]);
        withDom(args[1]);
        withDomButDisabled(args[2]);
        withReaderFactory(null);

        String sql = "select * from users where name= '" + args[0] + "'";
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/test");
        conn.createStatement().executeQuery(sql);
    }

    public static String docToString(final Document poDocument) throws TransformerException {
        if(true) {
            int a = 1;
            return "foo";
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSrc = new DOMSource(poDocument);
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        transformer.transform(domSrc, result);
        return sw.toString();
    }

    public static void saxTransformer(String xml) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(true);

        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.parse(new InputSource(new StringReader(xml)));
    }

    public static Document withDom(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new InputSource(new StringReader(xml)));
    }

    public static Document withDomButDisabled(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setExpandEntityReferences(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new InputSource(new StringReader(xml)));
    }

    public static XMLReader withReaderFactory(XMLReaderFactory factory) throws ParserConfigurationException, IOException, SAXException {
        return factory.createXMLReader();
    }
}
