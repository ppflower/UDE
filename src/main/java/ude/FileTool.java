package ude;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileTool {

    public static List<String> readLinesFromFile(String filePath) {
        try {
            List<String> lines = new ArrayList<>();

            FileInputStream inputStream = new FileInputStream(filePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String str;
            while ((str = bufferedReader.readLine()) != null) {
                lines.add(str);
            }
            inputStream.close();
            bufferedReader.close();
            return lines;
        } catch (Exception e) {
            return null;
        }
    }

    public static void addLine(String filePath, String content) {
        BufferedWriter out = null ;
        try {
            out = new BufferedWriter( new OutputStreamWriter(
                    new FileOutputStream(filePath, true )));
            out.write(content + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addLines(String filePath, List<String> lines) {
        BufferedWriter out = null ;
        try {
            out = new BufferedWriter( new OutputStreamWriter(
                    new FileOutputStream(filePath, true )));
            for (String line: lines) {
                out.write(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
