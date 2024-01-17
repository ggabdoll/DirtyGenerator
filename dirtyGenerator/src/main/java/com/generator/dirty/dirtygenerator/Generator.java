package com.generator.dirty.dirtygenerator;

import static org.codehaus.plexus.util.StringUtils.capitalizeFirstLetter;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.util.PsiUtilBase;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;

public class Generator extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {

    Editor editor = e.getData(PlatformDataKeys.EDITOR);
    if (editor == null) {
      Messages.showErrorDialog("Please open a file in the editor.", "Error");
      return;
    }

    Caret caret = editor.getCaretModel().getPrimaryCaret();
    PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(caret, e.getProject());
    if (psiFile == null) {
      Messages.showErrorDialog("Please open a file in the editor.", "Error");
      return;
    }

    // Java 파일인지 확인
    if (!(psiFile instanceof PsiJavaFile)) {
      Messages.showErrorDialog("Please open a Java file.", "Error");
      return;
    }
    PsiJavaFile javaFile = (PsiJavaFile) psiFile;
    PsiClass psiClass = javaFile.getClasses()[0]; // 첫 번째 클래스를 대상으로 가정

    // 필드들을 가져와 선택 가능한 UI 생성
    PsiField[] fields = psiClass.getFields();
    List<PsiField> selectedFields = showFieldSelectionDialog(fields, e.getProject());
    if (selectedFields.isEmpty()) {
      Messages.showInfoMessage("No fields selected.", "Code Generation");
      return;
    }

    // dirtyFiled
    // setterMethod 생성
    for (PsiField selectedField : selectedFields) {

      createDirtyFiled(psiClass, selectedField);
      createSetterMethod(psiClass, selectedField);
    }

    Messages.showInfoMessage("dirty generated successfully!", "Code Generation");

    // TODO: insert action logic here
  }

  private List<PsiField> showFieldSelectionDialog(PsiField[] fields, Project project) {
    DialogBuilder builder = new DialogBuilder(project);

    // 리스트 박스에 필드 목록 추가
    DefaultListModel<PsiField> listModel = new DefaultListModel<>();
    for (PsiField field : fields) {
      listModel.addElement(field);
    }

    JList<PsiField> fieldList = new JList<>(listModel);
    builder.setTitle("Select Fields");
    builder.setCenterPanel(new JScrollPane(fieldList));

    builder.addOkAction();
    builder.addCancelAction();

    if (builder.show() == DialogWrapper.OK_EXIT_CODE) {
      // 사용자가 OK를 누른 경우 선택한 필드 목록 반환
      return Arrays.asList(fieldList.getSelectedValuesList().toArray(new PsiField[0]));
    } else {
      return List.of(); // 취소된 경우 빈 목록 반환
    }
  }


  private void createDirtyFiled(PsiClass psiClass, PsiField field) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());

    String dirtyText = "@JsonIgnore \n"
        + "Boolean " + "is" + capitalizeFirstLetter(field.getName()) + "Dirty = false;";

    PsiField dirtyFiled = elementFactory.createFieldFromText(dirtyText, psiClass);

    // Document 변경 작업을 포함한 PSI 변경 작업
    PsiFile psiFile = psiClass.getContainingFile();
    Project project = psiClass.getProject();
    Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

    WriteCommandAction.runWriteCommandAction(project, () -> {
      // PSI 변경 작업 수행
      psiClass.add(dirtyFiled);

      // Document 변경 작업 수행 (선택적)
      if (document != null) {
        document.setText(psiFile.getText());
      }
    });

    // psiClass.add(dirtyFiled);
  }


  private void createSetterMethod(PsiClass psiClass, PsiField field) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());

    // Setter 메소드의 이름 및 매개변수 타입
    String setterMethodName = "set" + capitalizeFirstLetter(field.getName());
    String parameterType = field.getType().getCanonicalText();

    // 이미 같은 이름의 메소드가 있는지 확인
//    PsiMethod[] existingMethods = psiClass.findMethodsByName(setterMethodName, true);
//    for (PsiMethod existingMethod : existingMethods) {
//      PsiParameter[] parameters = existingMethod.getParameterList().getParameters();
//      if (parameters.length == 1 && parameters[0].getType().getCanonicalText()
//          .equals(parameterType)) {
//        // 이미 같은 시그니처의 메소드가 있으면 필드 변경 코드를 추가
//        addDirtyFieldAssignment(existingMethod, psiClass, field);
//        return;
//      }
//    }

    // Setter 메소드 생성
    String setterText =
        "public void " + setterMethodName + "(" + parameterType + " " + field.getName() + ") {" +
            "    this." + field.getName() + " = " + field.getName() + ";" +
            "    this.is" + capitalizeFirstLetter(field.getName()) + "Dirty = true;" +
            "}";

    PsiMethod setterMethod = elementFactory.createMethodFromText(setterText, psiClass);

    // Document 변경 작업을 포함한 PSI 변경 작업
    PsiFile psiFile = psiClass.getContainingFile();
    Project project = psiClass.getProject();
    Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

    WriteCommandAction.runWriteCommandAction(project, () -> {
      // PSI 변경 작업 수행
      psiClass.add(setterMethod);

      // Document 변경 작업 수행 (선택적)
      if (document != null) {
        document.setText(psiFile.getText());
      }
    });

//    // 메소드를 클래스에 추가
//    psiClass.add(setterMethod);
  }

  private void addDirtyFieldAssignment(PsiMethod setterMethod, PsiClass psiClass, PsiField field) {
    // 이미 있는 Setter 메소드에 필드 변경 코드를 추가
    PsiStatement[] statements = setterMethod.getBody().getStatements();
    String fieldName = field.getName();
    String assignmentText = "this.is" + capitalizeFirstLetter(fieldName) + "Dirty = true;";

    // 필드 변경 코드가 이미 있는지 확인
    for (PsiStatement statement : statements) {
      if (statement.getText().equals(assignmentText)) {
        // 이미 있는 경우 중복 추가를 피하기 위해 종료
        return;
      }
    }

    // 필드 변경 코드 추가
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
    PsiStatement assignmentStatement = elementFactory.createStatementFromText(assignmentText,
        psiClass);
    setterMethod.getBody().addBefore(assignmentStatement, null);
  }


}
