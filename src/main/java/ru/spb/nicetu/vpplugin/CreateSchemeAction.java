package ru.spb.nicetu.vpplugin;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.DiagramManager;
import com.vp.plugin.ViewManager;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import com.vp.plugin.diagram.IClassDiagramUIModel;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.IERDiagramUIModel;
import com.vp.plugin.model.IDBForeignKey;
import com.vp.plugin.model.IDBTable;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.factory.IModelElementFactory;

/**
 * @author vag
 *
 */
public class CreateSchemeAction implements VPActionController{

    @Override
    public void performAction(VPAction action) {
        // get the view manager and the parent component for modal the dialog.
        ViewManager viewManager = ApplicationManager.instance().getViewManager();
        Component parentFrame = viewManager.getRootFrame();
            
        // popup a file chooser for choosing the output file
        JFileChooser fileChooser = viewManager.createJFileChooser();
        fileChooser.setFileFilter(new FileFilter() {
            
            public String getDescription() {
                return "*.xsd";
            }
            
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(".xsd");
            }
            
        });
        fileChooser.showSaveDialog(parentFrame);
        File file = fileChooser.getSelectedFile();
        
        viewManager.showMessageDialog(parentFrame, "Мы выбрали файл");
        
        if (file!=null && !file.isDirectory()) {
            String result = "";
                    
            DiagramManager diagramManager = ApplicationManager.instance().getDiagramManager();
            IDiagramUIModel diagram = diagramManager.getActiveDiagram();
            
            if(diagram instanceof IERDiagramUIModel) {
                viewManager.showMessageDialog(parentFrame, "У нас ER диаграмма");
                
                result = generateXsdAndXmlFormEr((IERDiagramUIModel) diagram, fileChooser.getSelectedFile());
                
            } else if(diagram instanceof IClassDiagramUIModel) {
                viewManager.showMessageDialog(parentFrame, "У нас диаграмма классов");
            } else {
                viewManager.showMessageDialog(parentFrame, "Неизвестная диаграмма");
                viewManager.showMessageDialog(parentFrame, diagram.getType());
            }

            // show the generation result
            if (result.isEmpty()) {
                result = "Формирование файлов завершен корректно";
            } else {
                result = "Некорректное завершение. Имеются следующие ошибки: /n" + result;
            }
            viewManager.showMessageDialog(parentFrame, result);
        }
    }

    @Override
    public void update(VPAction action) {
        // TODO Auto-generated method stub
        
    }
    
/**
 * @param diagram
 * @param savedFile
 * @return
 */
//    private List<IDBTable> getTables(IERDiagramUIModel diagram) {
//        ViewManager viewManager = ApplicationManager.instance().getViewManager();
//        Component parentFrame = viewManager.getRootFrame();
//        
//        List<IDBTable> ret = new ArrayList<IDBTable>();
//        for(IDiagramElement shape : diagram.toDiagramElementArray()) {
////        for(IShapeUIModel shape : diagram.toShapeUIModelArray()) {
//            IModelElement element = shape.getModelElement();
//            if (element!= null) {
//                viewManager.showMessageDialog(parentFrame, "Есть элемент типа" + element.getModelType() + " с именем " + element.getName());
//            }
//            if(element != null && element.getModelType().equals(IModelElementFactory.MODEL_TYPE_DB_TABLE)) {
//                ret.add((IDBTable)element);
//            } else if(element != null && element.getModelType().equals(IModelElementFactory.MODEL_TYPE_DB_FOREIGN_KEY)) {
//                ret.add((IDBTable)element);
//            }
//        }
//        return ret;
//    }
    
    /**
     * 
     * @param diagram
     * @param savedFile
     * @return
     */
    public String generateXsdAndXmlFormEr(IERDiagramUIModel diagram, File savedFile) {
        String result = "";
        
        ViewManager viewManager = ApplicationManager.instance().getViewManager();
        Component parentFrame = viewManager.getRootFrame();
        List<IDBTable> tables = new ArrayList<IDBTable>();
        List<IDBForeignKey> relationships = new ArrayList<IDBForeignKey>();
        
        for(IDiagramElement shape : diagram.toDiagramElementArray()) {
              IModelElement element = shape.getModelElement();
              if(element != null && element.getModelType().equals(IModelElementFactory.MODEL_TYPE_DB_TABLE)) {
                  tables.add((IDBTable)element);
              } else if(element != null && element.getModelType().equals(IModelElementFactory.MODEL_TYPE_DB_FOREIGN_KEY)) {
                  relationships.add((IDBForeignKey)element);
              }
          }
        
        try {
            viewManager.showMessageDialog(parentFrame, "Создаем файлы");
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document xml = docBuilder.newDocument();
            
//            DocumentBuilderFactory docXsdFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder docXsdBuilder = docXsdFactory.newDocumentBuilder();
            Document xsd = docBuilder.newDocument();
            
            viewManager.showMessageDialog(parentFrame, "Создаем первые элементы");
        
            Element rootXmlElement = xml.createElement("model");
            xml.appendChild(rootXmlElement);
            
            Element rootXsdElement = xsd.createElement("xsd:schema");
            xsd.appendChild(rootXsdElement);
            
            Attr schemaAttr1 = xsd.createAttribute("xmlns:xsd");
            schemaAttr1.setValue("http://www.w3.org/2001/XMLSchema");
            rootXsdElement.setAttributeNode(schemaAttr1);
            
            Attr schemaAttr2 = xsd.createAttribute("targetNamespace");
            schemaAttr2.setValue("http://nicetu.spb.ru/space/types/1.0");
            rootXsdElement.setAttributeNode(schemaAttr2);
            
            Attr schemaAttr3 = xsd.createAttribute("xmlns");
            schemaAttr3.setValue("http://nicetu.spb.ru/space/types/1.0");
            rootXsdElement.setAttributeNode(schemaAttr3);
            
            Attr schemaAttr4 = xsd.createAttribute("xmlns:common-types");
            schemaAttr4.setValue("http://nicetu.spb.ru/common/types/1.0");
            rootXsdElement.setAttributeNode(schemaAttr4);
            
            Attr schemaAttr5 = xsd.createAttribute("elementFormDefault");
            schemaAttr5.setValue("qualified");
            rootXsdElement.setAttributeNode(schemaAttr5);
            
            viewManager.showMessageDialog(parentFrame, "Создаем остальные элементы");
            
            for(IDBTable classElement : tables) {
                
//                if(classElement.getName().contains(" ")) {
//                    //error
//                } else {
                    Element xmlElement = xml.createElement("object");
                    rootXmlElement.appendChild(xmlElement);
                    
                    Attr attr = xml.createAttribute("class");
                    attr.setValue(classElement.getName());
                    xmlElement.setAttributeNode(attr);
                    
                    Element xsdElement = xsd.createElement("xsd:complexType");
                    rootXsdElement.appendChild(xsdElement);
                    
                    Attr asdAttr = xsd.createAttribute("name");
                    asdAttr.setValue(classElement.getName());
                    xsdElement.setAttributeNode(asdAttr);
                    
//                    viewManager.showMessageDialog(parentFrame, "Класс: " + classElement.getName());
                    
                    for(IDBForeignKey relationship : relationships) {
                        
                        if (relationship.getFrom().equals(classElement)) {
//                        viewManager.showMessageDialog(parentFrame, "Связь у класса: " + classElement.getName());
                        
                        Element property = xml.createElement("property");
                        xmlElement.appendChild(property);
                        
//                        relationship.getOppositeEnd().getModelElement().getName();
                    
//                        viewManager.showMessageDialog(parentFrame, relationship.getTo());
                        if (relationship.getTo() != null) {
                            Attr attr1 = xml.createAttribute("class");
                            attr1.setValue(relationship.getTo().getName());
                            property.setAttributeNode(attr1);
                        }
                    
                        Attr attr2 = xml.createAttribute("comment");
                        attr2.setValue("");
                        property.setAttributeNode(attr2);
                    
//                        viewManager.showMessageDialog(parentFrame, relationship.getName());
                        Attr attr3 = xml.createAttribute("name");
                        attr3.setValue(relationship.getName());
                        property.setAttributeNode(attr3);
                    
                        Attr attr4 = xml.createAttribute("type");
                        attr4.setValue("");
                        property.setAttributeNode(attr4);
                        }
                    }
//                }
            }
            
            viewManager.showMessageDialog(parentFrame, "Всё создали. Начинаем сохранять");
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            
            viewManager.showMessageDialog(parentFrame, "Сохраняем xsd");
            
            DOMSource sourceXsd = new DOMSource(xsd);
            StreamResult streamXsdResult = new StreamResult(savedFile);
        
            transformer.transform(sourceXsd, streamXsdResult);
            
            viewManager.showMessageDialog(parentFrame, "Сохраняем xml");
            
            DOMSource sourceXml = new DOMSource(xml);
            StreamResult streamXmlResult = new StreamResult(new File(savedFile.getParentFile(),savedFile.getName()+".xml"));
        
            transformer.transform(sourceXml, streamXmlResult);
        
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
        
        return result;
    }

}
