package com.googlecode.jmeter.plugins.webdriver.sampler.gui;

import org.apache.commons.io.FileUtils;

import java.io.*;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

public class TestCaretListener implements CaretListener {
    private String initialContents;
    private String newContents;
    private JSyntaxTextArea textArea;

    TestCaretListener(JSyntaxTextArea textArea) {
        this.textArea = textArea;
    }

    public String getNewContents() {
        return newContents;
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        newContents = textArea.getText();
        if (initialContents != newContents) {
            initialContents = newContents;
//            try {
//                FileUtils.writeStringToFile(new File("test.js"),newContents);
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
            Process process = null;
            try {
               process = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", " /c C:\\Users\\l.lehadus\\AppData\\Roaming\\npm\\jshint.cmd C:\\Users\\l.lehadus\\Desktop\\jshinttest\\test.js").start();

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            InputStream inputStream = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(inputStreamReader);

            OutputStream outputStream = process.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

            try {
                bufferedWriter.write(newContents);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            String line;
            String linterOutput = "";

            try {
                while ((line = br.readLine()) != null) {
                    linterOutput += line;
                }
            } catch (IOException ex) {

            }
        }
    }
}
