/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.bpmn.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.activiti.bpmn.converter.child.BaseChildElementParser;
import org.activiti.bpmn.converter.util.BpmnXMLUtil;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.UserTask;
import org.activiti.bpmn.model.alfresco.AlfrescoUserTask;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Tijs Rademakers
 */
public class UserTaskXMLConverter extends BaseBpmnXMLConverter {
  
  List<String> formTypes = new ArrayList<String>();
  
  public UserTaskXMLConverter() {
    HumanPerformerParser humanPerformerParser = new HumanPerformerParser();
    childElementParsers.put(humanPerformerParser.getElementName(), humanPerformerParser);
    PotentialOwnerParser potentialOwnerParser = new PotentialOwnerParser();
    childElementParsers.put(potentialOwnerParser.getElementName(), potentialOwnerParser);
  }
  
  public static String getXMLType() {
    return ELEMENT_TASK_USER;
  }
  
  public static Class<? extends BaseElement> getBpmnElementType() {
    return UserTask.class;
  }
  
  @Override
  protected String getXMLElementName() {
    return ELEMENT_TASK_USER;
  }
  
  @Override
  protected BaseElement convertXMLToElement(XMLStreamReader xtr) throws Exception {
    String formKey = xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_FORM_FORMKEY);
    UserTask userTask = null;
    if (StringUtils.isNotEmpty(formKey)) {
      if (formTypes.contains(formKey)) {
        userTask = new AlfrescoUserTask();
      }
    }
    if (userTask == null) {
      userTask = new UserTask();
    }
    BpmnXMLUtil.addXMLLocation(userTask, xtr);
    userTask.setDueDate(xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_USER_DUEDATE));
    userTask.setFormKey(formKey);
    userTask.setAssignee(xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_USER_ASSIGNEE)); 
    userTask.setOwner(xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_USER_OWNER));
    userTask.setPriority(xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_USER_PRIORITY));
    
    if (StringUtils.isNotEmpty(xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_USER_CANDIDATEUSERS))) {
      String expression = xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_USER_CANDIDATEUSERS);
      userTask.getCandidateUsers().addAll(parseDelimitedList(expression));
    } 
    
    if (StringUtils.isNotEmpty(xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_USER_CANDIDATEGROUPS))) {
      String expression = xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_USER_CANDIDATEGROUPS);
      userTask.getCandidateGroups().addAll(parseDelimitedList(expression));
    }
    
    parseChildElements(getXMLElementName(), userTask, xtr);
    
    return userTask;
  }

  @Override
  protected void writeAdditionalAttributes(BaseElement element, XMLStreamWriter xtw) throws Exception {
    UserTask userTask = (UserTask) element;
    writeQualifiedAttribute(ATTRIBUTE_TASK_USER_ASSIGNEE, userTask.getAssignee(), xtw);
    writeQualifiedAttribute(ATTRIBUTE_TASK_USER_OWNER, userTask.getOwner(), xtw);
    writeQualifiedAttribute(ATTRIBUTE_TASK_USER_CANDIDATEUSERS, convertToDelimitedString(userTask.getCandidateUsers()), xtw);
    writeQualifiedAttribute(ATTRIBUTE_TASK_USER_CANDIDATEGROUPS, convertToDelimitedString(userTask.getCandidateGroups()), xtw);
    writeQualifiedAttribute(ATTRIBUTE_TASK_USER_DUEDATE, userTask.getDueDate(), xtw);
    writeQualifiedAttribute(ATTRIBUTE_FORM_FORMKEY, userTask.getFormKey(), xtw);
    if (userTask.getPriority() != null) {
      writeQualifiedAttribute(ATTRIBUTE_TASK_USER_PRIORITY, userTask.getPriority().toString(), xtw);
    }
  }
  
  @Override
  protected void writeExtensionChildElements(BaseElement element, XMLStreamWriter xtw) throws Exception {
    UserTask userTask = (UserTask) element;
    writeFormProperties(userTask, xtw);
  }

  @Override
  protected void writeAdditionalChildElements(BaseElement element, XMLStreamWriter xtw) throws Exception {
  }
  
  public void addFormType(String formType) {
    if (StringUtils.isNotEmpty(formType)) {
      formTypes.add(formType);
    }
  }
  
  public class HumanPerformerParser extends BaseChildElementParser {

    public String getElementName() {
      return "humanPerformer";
    }

    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
      UserTask userTask = (UserTask) parentElement;
      if (StringUtils.isNotEmpty(userTask.getAssignee())) {
        model.addProblem("No duplicate assignee and humanPerformer definition allowed", xtr);
      }
      String resourceElement = XMLStreamReaderUtil.moveDown(xtr);
      if (StringUtils.isNotEmpty(resourceElement) && "resourceAssignmentExpression".equals(resourceElement)) {
        String expression = XMLStreamReaderUtil.moveDown(xtr);
        if (StringUtils.isNotEmpty(expression) && "formalExpression".equals(expression)) {
          ((UserTask) parentElement).setAssignee(xtr.getElementText());
        }
      }
    }
  }
  
  public class PotentialOwnerParser extends BaseChildElementParser {

    public String getElementName() {
      return "potentialOwner";
    }

    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
      String resourceElement = XMLStreamReaderUtil.moveDown(xtr);
      if (StringUtils.isNotEmpty(resourceElement) && "resourceAssignmentExpression".equals(resourceElement)) {
        String expression = XMLStreamReaderUtil.moveDown(xtr);
        if (StringUtils.isNotEmpty(expression) && "formalExpression".equals(expression)) {
          List<String> assignmentList = new ArrayList<String>();
          String assignmentText = xtr.getElementText();
          if (assignmentText.contains(",")) {
            String[] assignmentArray = assignmentText.split(",");
            assignmentList = Arrays.asList(assignmentArray);
          } else {
            assignmentList.add(assignmentText);
          }
          for (String assignmentValue : assignmentList) {
            if (assignmentValue == null)
              continue;
            assignmentValue = assignmentValue.trim();
            if (assignmentValue.length() == 0)
              continue;

            String userPrefix = "user(";
            String groupPrefix = "group(";
            if (assignmentValue.startsWith(userPrefix)) {
              assignmentValue = assignmentValue.substring(userPrefix.length(), assignmentValue.length() - 1).trim();
              ((UserTask) parentElement).getCandidateUsers().add(assignmentValue);
            } else if (assignmentValue.startsWith(groupPrefix)) {
              assignmentValue = assignmentValue.substring(groupPrefix.length(), assignmentValue.length() - 1).trim();
              ((UserTask) parentElement).getCandidateGroups().add(assignmentValue);
            } else {
              ((UserTask) parentElement).getCandidateGroups().add(assignmentValue);
            }
          }
        }
      }
    }
  }
}
