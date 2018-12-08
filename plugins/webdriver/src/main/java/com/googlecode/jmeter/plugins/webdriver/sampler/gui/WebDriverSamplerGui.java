package com.googlecode.jmeter.plugins.webdriver.sampler.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import javax.swing.*;

import org.apache.commons.io.FileUtils;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JSR223BeanInfoSupport;

import com.googlecode.jmeter.plugins.webdriver.sampler.WebDriverSampler;

import kg.apc.jmeter.JMeterPluginsUtils;
import org.omg.CORBA.Environment;
import sun.misc.JavaLangAccess;

public class WebDriverSamplerGui extends AbstractSamplerGui {

    private static final long serialVersionUID = 100L;

    JTextField parameters;

    com.googlecode.jmeter.plugins.webdriver.sampler.gui.JSyntaxTextArea script;
    JComboBox<String> languages;
    JButton buildButton;
    TextArea output;
    JTextField cmdPath;
    JTextField jshintPath;
    JPanel jshintPanel;
    JPanel cmdPanel;
    JPanel errorsPanel;

    public WebDriverSamplerGui() {
        createGui();
    }

    @Override
    public String getStaticLabel() {
        return JMeterPluginsUtils.prefixLabel("WebDriver Sampler");
    }

    @Override
    public String getLabelResource() {
        return getClass().getCanonicalName();
    }

    @Override
    public void configure(TestElement element) {
        script.setText(element.getPropertyAsString(WebDriverSampler.SCRIPT));
        parameters.setText(element.getPropertyAsString(WebDriverSampler.PARAMETERS));
        cmdPath.setText(element.getPropertyAsString(WebDriverSampler.CMD_PATH));
        jshintPath.setText(element.getPropertyAsString(WebDriverSampler.JSHINT_PATH));
        languages.setSelectedItem(element.getPropertyAsString(WebDriverSampler.SCRIPT_LANGUAGE));
        super.configure(element);
    }

    @Override
    public TestElement createTestElement() {
        WebDriverSampler sampler = new WebDriverSampler();
        modifyTestElement(sampler);
        return sampler;
    }

    @Override
    public void modifyTestElement(TestElement element) {
        element.clear();
        this.configureTestElement(element);
        element.setProperty(WebDriverSampler.SCRIPT, script.getText());
        element.setProperty(WebDriverSampler.PARAMETERS, parameters.getText());
        element.setProperty(WebDriverSampler.SCRIPT_LANGUAGE, (String) languages.getSelectedItem());
        element.setProperty(WebDriverSampler.CMD_PATH, cmdPath.getText());
        element.setProperty(WebDriverSampler.JSHINT_PATH, jshintPath.getText());
    }

    @Override
    public void clearGui() {
        super.clearGui();

        parameters.setText("");
        script.setText(WebDriverSampler.defaultScript);
        parameters.setText("");
        script.setText(WebDriverSampler.defaultScript);
        languages.setSelectedItem(WebDriverSampler.DEFAULT_ENGINE);
        cmdPath.setText("");
        jshintPath.setText("");
    }

    private void createGui() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        Box box = Box.createVerticalBox();
        box.add(JMeterPluginsUtils.addHelpLinkToPanel(makeTitlePanel(), "WebDriverSampler"));
        box.add(createParameterPanel());
        box.add(createLangPanel());
        box.add(createCmdPanel());
        box.add(createJshintPanel());
        box.add(createBuildButton());

        add(box, BorderLayout.NORTH);


        JPanel panel = createScriptPanel();
        add(panel, BorderLayout.CENTER);

        JPanel panelErrors = createErrorsPanel();
        add(panelErrors, BorderLayout.SOUTH);

        // Don't let the input field shrink too much
        add(Box.createVerticalStrut(panel.getPreferredSize().height), BorderLayout.WEST);
        output.setForeground(Color.red);
        output.setBackground(Color.black);
    }

    private JPanel createParameterPanel() {
        final JLabel label = new JLabel("Parameters:");

        parameters = new JTextField(10);
        parameters.setName(WebDriverSampler.PARAMETERS);
        label.setLabelFor(parameters);

        final JPanel parameterPanel = new JPanel(new BorderLayout(5, 0));
        parameterPanel.add(label, BorderLayout.WEST);
        parameterPanel.add(parameters, BorderLayout.CENTER);

        return parameterPanel;
    }

    private JPanel createLangPanel() {
        final JLabel label = new JLabel("Script Language:");

        String[][] languageNames = JSR223BeanInfoSupport.LANGUAGE_NAMES;
        String[] langs = new String[languageNames.length];
        for (int n = 0; n < languageNames.length; n++) {
            langs[n] = languageNames[n][0];
        }

        languages = new JComboBox<>(langs);
        languages.setName(WebDriverSampler.PARAMETERS);
        label.setLabelFor(languages);
        languages.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JComboBox<String> source = (JComboBox<String>) actionEvent.getSource();
                String ctype = (String)source.getSelectedItem();
                setScriptContentType(ctype);
            }
        });

        final JPanel parameterPanel = new JPanel(new BorderLayout(5, 0));
        parameterPanel.add(label, BorderLayout.WEST);
        parameterPanel.add(languages, BorderLayout.CENTER);

        return parameterPanel;
    }

    private void setScriptContentType(String ctype) {
        output.setVisible(false);
        jshintPanel.setVisible(false);
        buildButton.setVisible(false);
        output.setText("");

        String text = script.getText();
        script.setLanguage(ctype.toLowerCase());
        script.setText(text);

        if (ctype.toLowerCase().equals("javascript") || ctype.toLowerCase().equals("js"))
        {
            cmdPanel.setVisible(true);
            jshintPanel.setVisible(true);
            buildButton.setVisible(true);
            output.setVisible(true);
        }
    }

    private JPanel createScriptPanel() {
        script =  JSyntaxTextArea.getInstance(25, 80, false);

        final JPanel panel = new JPanel(new BorderLayout());

        JTextArea linterOutput = new JTextArea("");
        linterOutput.setLineWrap(true);
        linterOutput.setEditable(true);
        linterOutput.setBackground(this.getBackground());
        panel.add(linterOutput, BorderLayout.SOUTH);

        TestCaretListener testCaretListener = new TestCaretListener(script);
        script.addCaretListener(testCaretListener);
        linterOutput.setText(testCaretListener.getNewContents());


        final JScrollPane scrollPane = JTextScrollPane.getInstance(script, true);
        setScriptContentType("text");
        script.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        final JLabel label = new JLabel("Script (see below for variables that are defined)");
        label.setLabelFor(script);

        panel.add(label, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        final JTextArea explain = new JTextArea("The following variables are defined for the script: WDS.name, WDS.parameters, WDS.args, WDS.vars, WDS.ctx, WDS.props, WDS.log, WDS.browser, WDS.sampleResult");
        explain.setLineWrap(true);
        explain.setEditable(false);
        explain.setBackground(this.getBackground());
        panel.add(explain, BorderLayout.SOUTH);

        return panel;
    }

     private JButton createBuildButton(){
         buildButton = new JButton("Build");
         output = new TextArea("");
         buildButton.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 String linterOutput = "";
                  String initialContents = null;
                  String newContents;
                  JSyntaxTextArea textArea;
                 newContents = script.getText();
                 String path = null;
                 String pathEscaped = null;
                 String jshintExec = null;
                 if (initialContents != newContents) {
                     initialContents = newContents;
                     try {
                         File file = File.createTempFile("test", "js");
                         FileUtils.writeStringToFile(file, script.getText());

                        path= file.getAbsolutePath();

                     } catch (IOException e1) {
                         e1.printStackTrace();
                     }
                     jshintExec =" /c " + jshintPath.getText() + " " + path;
                     Process process = null;

                     try {
                        process = new ProcessBuilder(cmdPath.getText(), jshintExec).start();
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


                     try {
                         while ((line = br.readLine()) != null) {
                             linterOutput += line;
                         }
                     } catch (IOException ex) {

                     }
                 }

                 output.setText(linterOutput.replaceAll("[.]","\n"));
                     }
         });

         return buildButton;

     }

     private JPanel createCmdPanel(){
         final JLabel label = new JLabel("Cmd path:");

         cmdPath = new JTextField(10);
         cmdPath.setName(WebDriverSampler.CMD_PATH);
         label.setLabelFor(cmdPath);

         cmdPanel = new JPanel(new BorderLayout(5, 0));
         cmdPanel.add(label, BorderLayout.WEST);
         cmdPanel.add(cmdPath, BorderLayout.CENTER);

         return cmdPanel;
     }

    private JPanel createJshintPanel(){
        final JLabel label = new JLabel("JsHint path:");

        jshintPath = new JTextField(10);
        jshintPath.setName(WebDriverSampler.JSHINT_PATH);
        label.setLabelFor(jshintPath);

        jshintPanel = new JPanel(new BorderLayout(5, 0));
        jshintPanel.add(label, BorderLayout.WEST);
        jshintPanel.add(jshintPath, BorderLayout.CENTER);

        return jshintPanel;
    }

    private JPanel createErrorsPanel(){
       JPanel errorsPanel= new JPanel(new BorderLayout(5, 0));
        errorsPanel.add(output, BorderLayout.CENTER);
        return errorsPanel;
    }
}
