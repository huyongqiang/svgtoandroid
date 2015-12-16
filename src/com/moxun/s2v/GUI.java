package com.moxun.s2v;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.xml.XmlFile;
import com.moxun.s2v.message.ErrorMessage;
import com.moxun.s2v.utils.Logger;
import com.moxun.s2v.utils.ModulesUtil;
import com.moxun.s2v.utils.MyCellRender;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by moxun on 15/12/14.
 */
public class GUI {
    private JPanel rootPanel;
    private JComboBox dpiChooser;
    private JTextField svgPath;
    private JButton svgSelectBtn;
    private JComboBox moduleChooser;
    private JButton generateButton;
    private JTextField xmlName;
    private JFrame frame;

    private Project project;
    private final String DRAWABLE = "drawable";
    private Set<String> distDirList = new HashSet<String>();
    private ModulesUtil modulesUtil;

    private XmlFile svg;

    public GUI(Project project) {
        this.project = project;
        frame = new JFrame("SVG to VectorDrawable");
        modulesUtil = new ModulesUtil(project);
        distDirList.clear();
        setListener();
        initModules();
    }

    private void initModules() {
        for (String item : modulesUtil.getModules()) {
            moduleChooser.addItem(item);
        }
    }

    private void setListener() {
        svgSelectBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSVGChooser();
            }
        });

        moduleChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dpiChooser.setRenderer(new MyCellRender(modulesUtil.getExistDpiDirs(moduleChooser.getSelectedItem().toString())));
            }
        });

        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String moduleName = (String) moduleChooser.getSelectedItem();
                PsiDirectory resDir = modulesUtil.getResDir(moduleName);
                if (resDir != null) {
                    Logger.debug("Got res dir " + resDir.getVirtualFile().getPath());
                    Logger.debug("Existing drawable dirs " + modulesUtil.getDrawableDirs(resDir));
                }

                if (modulesUtil.isAndroidProject()) {
                    new Transformer.Builder()
                            .setProject(project)
                            .setSVG(svg)
                            .setDpi((String) dpiChooser.getSelectedItem())
                            .setModule(moduleName)
                            .setXmlName(xmlName.getText())
                            .create()
                            .transforming();
                } else {
                    ErrorMessage.show(project,"Current project is not an Android project!");
                }

            }
        });
    }

    private void showSVGChooser() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        VirtualFile virtualFile = FileChooser.chooseFile(descriptor, project, null);
        if (virtualFile != null) {
            if (!virtualFile.isDirectory() && virtualFile.getName().endsWith("svg")) {
                svg = (XmlFile) PsiManager.getInstance(project).findFile(virtualFile);
                //got *.svg file as xml
                svgPath.setText(virtualFile.getPath());
                xmlName.setText("vector_drawable_" + svg.getName().split("\\.")[0] + ".xml");
            } else {
                ErrorMessage.show(project,"Please choosing a SVG file.");
            }
        }
        frame.setAlwaysOnTop(true);
    }

    private void showXMLChooser() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        VirtualFile virtualFile = FileChooser.chooseFile(descriptor, project, null);
        if (virtualFile != null) {
            if (virtualFile.isDirectory() && virtualFile.getName().startsWith(DRAWABLE)) {
                PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile);
                PsiDirectory[] dirs = directory.getParentDirectory().getSubdirectories();
                for (PsiDirectory dir : dirs) {
                    if (dir.isDirectory() && dir.getName().contains(DRAWABLE)) {
                        System.out.println(dir.getName() + " is dist dir");
                        if (dir.getName().equals(DRAWABLE)) {
                            distDirList.add("nodpi");
                        } else {
                            String[] tmp = dir.getName().split("-");
                            if (tmp.length == 2) {
                                distDirList.add(tmp[1]);
                            }
                        }
                    }
                }
                System.out.println(distDirList.toString());
                //String template = FileTemplateManager.getInstance(project).findInternalTemplate("vector").getText();
                //XmlFile xml = (XmlFile) PsiFileFactory.getInstance(project).createFileFromText("export.xml", StdFileTypes.XML,template);
                //got *.xml file as XmlFile
                //directory.add(xml);
                //System.out.println(xml.toString());
            } else {
                System.out.println(virtualFile.getName());
                //not a drawable dir
            }
        }
    }

    public void show() {
        frame.setContentPane(rootPanel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(frame.getParent());
        frame.setVisible(true);
    }
}
