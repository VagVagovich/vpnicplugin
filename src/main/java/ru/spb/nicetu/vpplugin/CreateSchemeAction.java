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
                
                generateXmlFormEr((IERDiagramUIModel) diagram, fileChooser.getSelectedFile());
                
            } else if(diagram instanceof IClassDiagramUIModel) {
                viewManager.showMessageDialog(parentFrame, "У нас диаграмма классов");
            } else {
                viewManager.showMessageDialog(parentFrame, "Неизвестная диаграмма");
                viewManager.showMessageDialog(parentFrame, diagram.getType());
            }

            // show the generation result
            viewManager.showMessageDialog(parentFrame, result);
        }
    }

    @Override
    public void update(VPAction action) {
        // TODO Auto-generated method stub
        
    }
    
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
    
    public void generateXmlFormEr(IERDiagramUIModel diagram, File savedFile) {
        
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
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        
            Document doc = docBuilder.newDocument();
        
            Element rootElement = doc.createElement("model");
            doc.appendChild(rootElement);
            
            for(IDBTable classElement : tables) {
                                
                Element staff = doc.createElement("object");
                rootElement.appendChild(staff);
            
                Attr attr = doc.createAttribute("class");
                attr.setValue(classElement.getName());
                staff.setAttributeNode(attr);
                
//                viewManager.showMessageDialog(parentFrame, "Класс: " + classElement.getName());
                
//                for(IDBColumn column : classElement.toDBColumnArray()) {
//                    viewManager.showMessageDialog(parentFrame, "Параметр: " + column.getName() + "тип " + column.getType() + " "  + column.getTypeInText());
//                }
                
//                for(IChartRelationship relationship : classElement.toFromChartRelationshipArray()) {
//                    viewManager.showMessageDialog(parentFrame, "Просто связь у класса: " + classElement.getName());
//                }
                
                for(IDBForeignKey relationship : relationships) {
                    
                    if (relationship.getFrom().equals(classElement)) {
                    viewManager.showMessageDialog(parentFrame, "Связь у класса: " + classElement.getName());
                    
                    Element property = doc.createElement("property");
                    staff.appendChild(property);
                    
//                    relationship.getOppositeEnd().getModelElement().getName();
                
//                    viewManager.showMessageDialog(parentFrame, relationship.getTo());
                    if (relationship.getTo() != null) {
                        Attr attr1 = doc.createAttribute("class");
                        attr1.setValue(relationship.getTo().getName());
                        property.setAttributeNode(attr1);
                    }
                
                    Attr attr2 = doc.createAttribute("comment");
                    attr2.setValue("");
                    property.setAttributeNode(attr2);
                
//                    viewManager.showMessageDialog(parentFrame, relationship.getName());
                    Attr attr3 = doc.createAttribute("name");
                    attr3.setValue(relationship.getName());
                    property.setAttributeNode(attr3);
                
                    Attr attr4 = doc.createAttribute("type");
                    attr4.setValue("");
                    property.setAttributeNode(attr4);
                    }
                }
            }
        
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(savedFile);
        
            transformer.transform(source, streamResult);
        
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
        
    }

}
