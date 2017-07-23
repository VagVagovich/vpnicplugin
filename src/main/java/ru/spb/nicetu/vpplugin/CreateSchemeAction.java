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
import javax.xml.transform.OutputKeys;
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
import com.vp.plugin.model.IAssociation;
import com.vp.plugin.model.IAttribute;
import com.vp.plugin.model.IClass;
import com.vp.plugin.model.IDBColumn;
import com.vp.plugin.model.IDBForeignKey;
import com.vp.plugin.model.IDBTable;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.factory.IModelElementFactory;

/**
 * Схема работы плагина к VP
 * @author Валерий Голубев
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
                result = generateXsdAndXmlFormClassDiagram((IClassDiagramUIModel) diagram, fileChooser.getSelectedFile());
            } else {
                viewManager.showMessageDialog(parentFrame, "Неизвестная диаграмма");
                result = "Неверная диаграмма типа " + diagram.getType();
            }

            // show the generation result
            if (result.isEmpty()) {
                result = "Формирование файлов завершен корректно";
            } else {
                result = "Некорректное завершение. Имеются следующие ошибки: " + result;
            }
            viewManager.showMessageDialog(parentFrame, result);
        }
    }

    @Override
    public void update(VPAction action) {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * Сгенерировать модель по диаграмме классов
     * @param diagram - диаграмма классов
     * @param savedFile - место сохранения
     * @return строка с обратной связью для пользователя с результатом работы
     */
    public String generateXsdAndXmlFormClassDiagram(IClassDiagramUIModel diagram, File savedFile) {
        String result = "";
        
        ViewManager viewManager = ApplicationManager.instance().getViewManager();
        Component parentFrame = viewManager.getRootFrame();
        List<IClass> tables = new ArrayList<IClass>();
        List<IAssociation> relationships = new ArrayList<IAssociation>();
        
        for(IDiagramElement shape : diagram.toDiagramElementArray()) {
            IModelElement element = shape.getModelElement();
            if(element != null && element.getModelType().equals(IModelElementFactory.MODEL_TYPE_CLASS)) {
                tables.add((IClass)element);
            } else if(element != null && element.getModelType().equals(IModelElementFactory.MODEL_TYPE_ASSOCIATION)) {
                relationships.add((IAssociation)element);
            }
        }
        
        try {
            viewManager.showMessageDialog(parentFrame, "Создаем файлы");
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document xml = docBuilder.newDocument();
            
            
            Document xsd = docBuilder.newDocument();
            
            viewManager.showMessageDialog(parentFrame, "Создаем первые элементы");
        
            Element rootXmlElement = xml.createElement("model");
            xml.appendChild(rootXmlElement);
            
            Element rootXsdElement = createRootXsdEleent(xsd);
            
            viewManager.showMessageDialog(parentFrame, "Создаем остальные элементы");
            
            for(IClass classElement : tables) {
                
//                if(classElement.getName().contains(" ")) {
//                    //error
//                } else {
                    Element xmlElement = xml.createElement("object");
                    rootXmlElement.appendChild(xmlElement);
                    
                    Attr attr = xml.createAttribute("class");
                    attr.setValue(classElement.getName());
                    xmlElement.setAttributeNode(attr);
                    
                    createXsdClassClass(xsd, rootXsdElement, classElement);
                    
                    for(IAssociation relationship : relationships) {
                        
                        if (relationship.getFrom().equals(classElement)) {
                        
                            Element property = xml.createElement("property");
                            xmlElement.appendChild(property);
                        
                            if (relationship.getTo() != null) {
                                Attr attr1 = xml.createAttribute("class");
                                attr1.setValue(relationship.getTo().getName());
                                property.setAttributeNode(attr1);
                            }
                            
                            Attr attr2 = xml.createAttribute("comment");
                            attr2.setValue("");
                            property.setAttributeNode(attr2);
                    
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
            
            DOMSource sourceXsd = new DOMSource(xsd);
            StreamResult streamXsdResult = new StreamResult(savedFile);
        
            transformer.transform(sourceXsd, streamXsdResult);
            
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
    
    /**
     * Создать классы для xsd файла по диаграмме классов
     * @param xsd - xsd файл
     * @param rootElement - корневой элемент
     * @param classElement - класс
     */
    private void createXsdClassClass(Document xsd, Element rootElement, IClass classElement){
        Element xsdElement = xsd.createElement("xsd:complexType");
        rootElement.appendChild(xsdElement);
        
        Attr xsdAttr = xsd.createAttribute("name");
        xsdAttr.setValue(classElement.getName());
        xsdElement.setAttributeNode(xsdAttr);
        
        Element xsdComplexContentElement = xsd.createElement("xsd:complexContent");
        xsdElement.appendChild(xsdComplexContentElement);
        
        Element xsdExtElement = xsd.createElement("xsd:extension");
        xsdComplexContentElement.appendChild(xsdExtElement);
        
        String prepareCommomTypesName = "common-types:objectType";
        
        for(IAttribute param : classElement.toAttributeArray()) {
            createClassParam(xsd, xsdExtElement, param);
            if ("name".equals(param.getName())) {
                prepareCommomTypesName = "common-types:namedObjectType";
            } else if ("classifire".equals(param.getName())) {
                prepareCommomTypesName = "common-types:uniqueObjectType";
            }
        }
        
        Attr xsdExtAttr = xsd.createAttribute("base");
        //TODO здесь могут быть другие типы
        xsdExtAttr.setValue(prepareCommomTypesName);
        xsdExtElement.setAttributeNode(xsdExtAttr);
    }
    
    /**
     * Создать параметры по диаграмме классов 
     * @param xsd - xsd файл
     * @param xsdElement - xsd элемент
     * @param param - параметры
     */
    private void createClassParam(Document xsd, Element xsdElement, IAttribute param){
        Element xsdAttributeElement = xsd.createElement("xsd:attribute");
        xsdElement.appendChild(xsdAttributeElement);
        
        Attr xsdAtrAttr = xsd.createAttribute("name");
        xsdAtrAttr.setValue(param.getName());
        xsdAttributeElement.setAttributeNode(xsdAtrAttr);
        
        Attr xsdAtrTypeAttr = xsd.createAttribute("type");
        xsdAtrTypeAttr.setValue("xsd:" + param.getTypeAsText());
        xsdAttributeElement.setAttributeNode(xsdAtrTypeAttr);
        
        Attr xsdUseAttr = xsd.createAttribute("use");
        //TODO а откуда брать обязательность полей?
        xsdUseAttr.setValue("required");
        xsdAttributeElement.setAttributeNode(xsdUseAttr);
    }
    
    /**
     * Сформировать xsd и xml файлы по ER диаграмме
     * @param diagram - ER диаграмма
     * @param savedFile - место для сохраняемог файлы
     * @return результат
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
            
            Document xsd = docBuilder.newDocument();
            
            viewManager.showMessageDialog(parentFrame, "Создаем первые элементы");
        
            Element rootXmlElement = xml.createElement("model");
            xml.appendChild(rootXmlElement);
            
            Element rootXsdElement = createRootXsdEleent(xsd);
            
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
                    
                    createXsdErClass(xsd, rootXsdElement, classElement);
                    
                    for(IDBForeignKey relationship : relationships) {
                        
                        if (relationship.getFrom().equals(classElement)) {
                        
                            Element property = xml.createElement("property");
                            xmlElement.appendChild(property);
                        
                            if (relationship.getTo() != null) {
                                Attr attr1 = xml.createAttribute("class");
                                attr1.setValue(relationship.getTo().getName());
                                property.setAttributeNode(attr1);
                            }
                            
                            Attr attr2 = xml.createAttribute("comment");
                            attr2.setValue("");
                            property.setAttributeNode(attr2);
                    
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
            
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            
            DOMSource sourceXsd = new DOMSource(xsd);
            StreamResult streamXsdResult = new StreamResult(savedFile);
        
            transformer.transform(sourceXsd, streamXsdResult);
            
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
    
    /**
     * Создать классы для xsd файла по ER диаграмме
     * @param xsd - xsd файл
     * @param rootElement - корневой элемент
     * @param classElement - класс в ER диаграмме
     */
    private void createXsdErClass(Document xsd, Element rootElement, IDBTable classElement){
        Element xsdElement = xsd.createElement("xsd:complexType");
        rootElement.appendChild(xsdElement);
        
        Attr xsdAttr = xsd.createAttribute("name");
        xsdAttr.setValue(classElement.getName());
        xsdElement.setAttributeNode(xsdAttr);
        
        Element xsdComplexContentElement = xsd.createElement("xsd:complexContent");
        xsdElement.appendChild(xsdComplexContentElement);
        
        Element xsdExtElement = xsd.createElement("xsd:extension");
        xsdComplexContentElement.appendChild(xsdExtElement);
        
        String prepareCommomTypesName = "common-types:objectType";
        
        for(IDBColumn param : classElement.toDBColumnArray()) {
            createErParam(xsd, xsdExtElement, param);
            if ("name".equals(param.getName())) {
                prepareCommomTypesName = "common-types:namedObjectType";
            } else if ("classifire".equals(param.getName())) {
                prepareCommomTypesName = "common-types:uniqueObjectType";
            }
        }
        
        Attr xsdExtAttr = xsd.createAttribute("base");
        //TODO здесь могут быть другие типы
        xsdExtAttr.setValue(prepareCommomTypesName);
        xsdExtElement.setAttributeNode(xsdExtAttr);
    }
    
    /**
     * Создать параметры по ER диаграмме 
     * @param xsd - xsd файл
     * @param xsdElement - xsd элемент
     * @param param - параметры
     */
    private void createErParam(Document xsd, Element xsdElement, IDBColumn param){
        Element xsdAttributeElement = xsd.createElement("xsd:attribute");
        xsdElement.appendChild(xsdAttributeElement);
        
        Attr xsdAtrAttr = xsd.createAttribute("name");
        xsdAtrAttr.setValue(param.getName());
        xsdAttributeElement.setAttributeNode(xsdAtrAttr);
        
        Attr xsdAtrTypeAttr = xsd.createAttribute("type");
        xsdAtrTypeAttr.setValue("xsd:" + param.getTypeInText());
        xsdAttributeElement.setAttributeNode(xsdAtrTypeAttr);
        
        Attr xsdUseAttr = xsd.createAttribute("use");
        //TODO а откуда брать обязательность полей?
        xsdUseAttr.setValue("required");
        xsdAttributeElement.setAttributeNode(xsdUseAttr);
    }
    
    /**
     * Создать коневой элемент для xsd файла
     * @param xsd - Xsd документ
     * @return коневой элемент для xsd файла
     */
    private Element createRootXsdEleent(Document xsd){
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
        
        Element xsdImportElement = xsd.createElement("xsd:import");
        rootXsdElement.appendChild(xsdImportElement);
        
        Attr impAttr1 = xsd.createAttribute("namespace");
        impAttr1.setValue("http://nicetu.spb.ru/common/types/1.0");
        xsdImportElement.setAttributeNode(impAttr1);
        
        Attr impAttr2 = xsd.createAttribute("schemaLocation");
        impAttr2.setValue("common.xsd");
        xsdImportElement.setAttributeNode(impAttr2);
        
        Element xsdIncludeElement = xsd.createElement("xsd:include");
        rootXsdElement.appendChild(xsdIncludeElement);
        
        Attr inclAttr = xsd.createAttribute("schemaLocation");
        inclAttr.setValue("primitives.xsd");
        xsdIncludeElement.setAttributeNode(inclAttr);
        
        return rootXsdElement;
    }

}
