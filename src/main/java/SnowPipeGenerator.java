import domain.HeaderColumnMap;
import domain.SnowPipeObject;
import io.github.millij.poi.SpreadsheetReadException;
import io.github.millij.poi.ss.reader.XlsReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SnowPipeGenerator {
    private static final String JOIN_STR = ", ";
    private static final String SF_FILE_INDEX_PREFIX = "f.$";
    static List<HeaderColumnMap> headerColumnMapList = new ArrayList<>();

    private static String createSnowPipe(Map<String, String> headerColMap, String snowPipeConfigPath) {
        SnowPipeObject snowPipeObject = prepareSnowPipeObject(snowPipeConfigPath);
        if (snowPipeObject == null) {
            System.out.println("Can't read snowPipe config file");
            return null;
        }
        String columns = prepareColumns(headerColMap);
        System.out.println("Prepared Columns: " + columns);
        String values = prepareValues();
        System.out.println("Prepared Values: " + values);
        return " CREATE OR REPLACE PIPE " + snowPipeObject.getDb() + "." + snowPipeObject.getSchema() + "." + snowPipeObject.getPipeName() + " AS " +
                " COPY INTO " + snowPipeObject.getDb() + "." + snowPipeObject.getSchema() + "." + snowPipeObject.getTable() + " ( " + columns + " ) " +
                " FROM ( SELECT " + values +
                " FROM @" + snowPipeObject.getDb() + "." + snowPipeObject.getSchema() + "." + snowPipeObject.getStage() + " f) " +
                " FILE_FORMAT = ( FORMAT_NAME = " + snowPipeObject.getDb() + "." + snowPipeObject.getSchema() + "." + snowPipeObject.getFileFormat() + " ) on_error=continue";
    }

    private static SnowPipeObject prepareSnowPipeObject(String snowPipeConfigPath) {
        final File xlsFile = new File(snowPipeConfigPath);
        if (!xlsFile.exists()){
            System.out.println(snowPipeConfigPath+" Doesn't exist");
            return null;
        }
        SnowPipeObject snowPipeObject = null;
        final XlsReader reader = new XlsReader();
        try {
            List<SnowPipeObject> snowPipeObjects = reader.read(SnowPipeObject.class, xlsFile, 0);
            return snowPipeObjects.get(0);
        } catch (SpreadsheetReadException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<String, String> prepareHeaderColumnMap(String mappingFile) {
        final File xlsFile = new File(mappingFile);
        final XlsReader reader = new XlsReader();
        Map<String, String> headColMap = new LinkedHashMap<String, String>();
        try {
            headerColumnMapList = reader.read(HeaderColumnMap.class, xlsFile, 0);
            for (HeaderColumnMap headerColumnMap : headerColumnMapList) {
                if (headerColumnMap.getHeader()!=null){
                    headColMap.put(headerColumnMap.getHeader(), headerColumnMap.getColumn());
                }
            }
        } catch (SpreadsheetReadException e) {
            e.printStackTrace();
        }
        return headColMap;
    }

    private static String prepareValues() {
        List<String> values = new ArrayList<>();
        int index = 1;
        for (HeaderColumnMap headerColumnMap : headerColumnMapList) {
            if (headerColumnMap.getDefaultVal() == null && headerColumnMap.getColumn()!=null) {
                values.add(SF_FILE_INDEX_PREFIX + index);
                index++;
            } else if(headerColumnMap.getColumn()==null && headerColumnMap.getDefaultVal() == null){
                index++;
            }else if (headerColumnMap.getDefaultVal() != null && headerColumnMap.getIsString()) {
                String stringifyVal = headerColumnMap.getDefaultVal();
                stringifyVal = "'" + stringifyVal + "'";
                values.add(stringifyVal);
            } else if (headerColumnMap.getDefaultVal() != null && !headerColumnMap.getIsString()) {
                values.add(headerColumnMap.getDefaultVal());
            }
        }
        return values.stream().collect(Collectors.joining(JOIN_STR));
    }

    private static String prepareColumns(Map<String, String> headerColMap) {
        String columns = appendHeaderMatchingColumns(headerColMap);
        String auditColumns = headerColumnMapList
                .stream()
                .filter(hc -> hc.getHeader() == null)
                .map(h -> h.getColumn())
                .collect(Collectors.joining(JOIN_STR));
        columns = columns + "," + auditColumns;

        return columns;
    }

    private static String appendHeaderMatchingColumns(Map<String, String> headerColMap) {
        List<String> columnList = new ArrayList<>();
        Set<String> headers = headerColMap.keySet();
        for (String header : headers) {
            if (header != null && !header.isEmpty()) {
                columnList.add(headerColMap.get(header));
            }else {
                continue;
            }

        }
        return columnList.stream().filter(c->c!=null).collect(Collectors.joining(JOIN_STR));
    }

    public boolean generatePipe(String pipeConfigFile, String mappingFile,String fileName,String generateInDb) {
        String snowPipeSql = createSnowPipe(prepareHeaderColumnMap(mappingFile), pipeConfigFile);
        if (snowPipeSql==null){
            return false;
        }
        System.out.println("---------------------\n"+snowPipeSql+"\n-----------------------\n");
        try {
            File pipeFile = new File(fileName);
            if (pipeFile.exists()){
                pipeFile.delete();
                System.out.println("deleting old file");
            }
            Files.write(Paths.get(pipeFile.getPath()), snowPipeSql.getBytes());
            if (!pipeFile.exists()){
                System.out.println("Failed to create "+pipeFile.getName());
            }
            if (generateInDb.equals("true")){
                if (executeSnowPipeSql(fileName, snowPipeSql)) return true;
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean executeSnowPipeSql(String fileName, String snowPipeSql) {
        try {
            Connection connection = getConnection();
            if (connection!=null) {
                System.out.println("Connection Established");
                boolean success = connection.prepareStatement(snowPipeSql).execute();
                if (success) {
                    System.out.println("SnowPipe created in DB with name: " + fileName);
                    return true;
                }
            }
        } catch (SQLException throwables) {
            System.out.println(throwables.getMessage());
            System.out.println("Failed to create SnowPipe in DB with name ");
        }

        System.out.println("Please execute "+ fileName +" Manually");
        return false;
    }

    private static Connection getConnection()
            throws SQLException {
        try {
            Class.forName("net.snowflake.client.jdbc.SnowflakeDriver");
        } catch (ClassNotFoundException ex) {
            System.err.println("Driver not found");
        }

        // build connection properties

        Properties properties = new Properties();
        InputStream inStream = null;
        try {
            String currentDirPath = new File(SnowPipeGenerator.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            String path = currentDirPath.substring(0,currentDirPath.lastIndexOf("\\"));
            path=path+"\\"+"dbConnection.properties";
            System.out.println(path);
            inStream = new FileInputStream(path);
            if (inStream==null){
                System.out.println("dbConnection.properties not found");
            }
            properties.load(inStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        java.lang.String jdbcUrl = "jdbc:snowflake://"+properties.getProperty("account")+".us-east-1.snowflakecomputing.com";
        return DriverManager.getConnection(jdbcUrl, properties);
    }
}
