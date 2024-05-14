package com.generator.dirty.dirtygenerator;

import static org.codehaus.plexus.util.StringUtils.capitalizeFirstLetter;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;

public class Generator extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {

    Editor editor = e.getData(PlatformDataKeys.EDITOR);
    if (editor == null) {
      Messages.showErrorDialog("Please open a file in the editor.", "Error");
      return;
    }

    Caret caret = editor.getCaretModel().getPrimaryCaret();

    int offset = caret.getOffset();

//    PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(caret, e.getProject());
    PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
    if (psiFile == null) {
      Messages.showErrorDialog("Please open a file in the editor.", "Error");
      return;
    }

    // Java 파일인지 확인
    if (!(psiFile instanceof PsiJavaFile)) {
      Messages.showErrorDialog("Please open a Java file.", "Error");
      return;
    }
    // ======== File Validation 끝

    // ======== Class Validation
    PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
    if (classes.length == 0) {
      Messages.showErrorDialog("No classes found in the file.", "Error");
      return;
    }

    //PsiJavaFile javaFile = (PsiJavaFile) psiFile;

//    PsiClass psiClass = classes[0];// 첫 번째 클래스를 대상으로 가정

    // 클래스 찾기
    PsiClass psiClass = findPsiClass(psiFile, offset);

    List<PsiField> fields = getAllFields(psiClass);

    if (fields.isEmpty()) {
      Messages.showInfoMessage("No fields found in the class.", "Info");
      return;
    }

    // 필드들을 가져와 선택 가능한 UI 생성
    //PsiField[] fields = psiClass.getFields();
    List<PsiField> selectedFields = showFieldSelectionDialog(psiClass, fields, e.getProject());
    if (selectedFields.isEmpty()) {
      //      Messages.showInfoMessage("No fields selected.", "Code Generation");
      return;
    }

    // dirtyFiled
    // setterMethod 생성
    for (
        PsiField selectedField : selectedFields) {

      createDirtyFiled(psiClass, selectedField);
      createSetterMethod(psiClass, selectedField);
    }

    //Messages.showInfoMessage("dirty generated successfully!", "Code Generation");
  }


  /*
   *  methodName  : findPsiClass
   *  description : offset을 기반으로 class를 찾는다.
   */
  private PsiClass findPsiClass(PsiFile psiFile, int offset) {
    // PsiFile 내에서 클래스 요소를 찾아 반환합니다.

    // innerClass에서 작동하도록
    PsiElement element = psiFile.findElementAt(offset);
    return PsiTreeUtil.getParentOfType(element, PsiClass.class);
  }

  private List<PsiField> getAllFields(PsiClass psiClass) {
    List<PsiField> fields = new ArrayList<>();
    PsiField[] psiFields = psiClass.getFields();
    fields.addAll(Arrays.asList(psiFields));
//    PsiClass[] innerClasses = psiClass.getInnerClasses();
//    for (PsiClass innerClass : innerClasses) {
//      fields.addAll(getAllFields(innerClass)); // Recursively fetch fields from inner classes
//    }
    return fields;
  }


  private List<PsiField> showFieldSelectionDialog(PsiClass psiClass, List<PsiField> fields,
      Project project) {

//    FieldSelectionDialog dialog = new FieldSelectionDialog(project, psiClass);
//
//    if (dialog.showAndGet()) {
//      return dialog.getSelectedFields();
//    } else {
//      return List.of();
//    }
    DialogBuilder builder = new DialogBuilder(project);

    // title
    builder.setTitle("Select Fields for Dirty Generation");

    // 클래스 이름
    JLabel classNameLabel = new JLabel("Class: " + psiClass.getQualifiedName());

    // 클래스 이름 위에 박아두기
    builder.setNorthPanel(classNameLabel);

    builder.getWindow().setBackground(Color.BLUE);

    // 리스트 박스에 필드 목록 추가
    DefaultListModel<PsiField> listModel = new DefaultListModel<>();

    fields.stream().filter(field -> !field.hasModifierProperty("static"))
        .forEach(listModel::addElement);
//    Arrays.stream(psiClass.getFields())
//        .filter(field -> !field.hasModifierProperty("static"))
//        .forEach(listModel::addElement);

    // JBList
    JBList<PsiField> fieldList = new JBList<>(listModel);

    // JBScrollpane
    JBScrollPane scrollPane = new JBScrollPane(fieldList);

    // 사용자 정의된 ListCellRenderer를 사용하여 필드 정보를 보여주기
    fieldList.setCellRenderer(new FieldListCellRenderer());

    scrollPane.setPreferredSize(new Dimension(400, 500));
    scrollPane.getViewport().setBackground(new Color(64, 164, 216));
    scrollPane.getViewport().setBackground(Color.blue);

    // builder에 set
    builder.setCenterPanel(new JBScrollPane(scrollPane));

    builder.getCenterPanel().setBackground(Color.blue);
    builder.getCenterPanel().setBackground(new Color(64, 164, 216));

    builder.getCenterPanel().getBackground().brighter();

    // TODO : 색을 바꿔보쟈

    // 버튼 action 할당
    builder.addOkAction();
    builder.addCancelAction();

//    JScrollPane scrollPane = new JScrollPane(fieldList);
//    scrollPane.setPreferredSize(new Dimension(400, 300));
//    builder.setCenterPanel(new JScrollPane(scrollPane));
//
//
    if (builder.show() == DialogWrapper.OK_EXIT_CODE) {
      // 사용자가 OK를 누른 경우 선택한 필드 목록 반환
      return fieldList.getSelectedValuesList();
//      return Arrays.asList(fieldList.getSelectedValuesList().toArray(new PsiField[0]));
    } else {
      return List.of(); // 취소된 경우 빈 목록 반환
    }
  }


  private void createDirtyFiled(PsiClass psiClass, PsiField field) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());

    String dirtyText = "@JsonIgnore \n"
        + "private Boolean " + "is" + capitalizeFirstLetter(field.getName()) + "Dirty = false;";

    PsiField dirtyFiled = elementFactory.createFieldFromText(dirtyText, psiClass);

    String commentText = "/* " + field.getName() + " */";

    PsiComment comment = elementFactory.createCommentFromText(commentText, psiClass);

    // Document 변경 작업을 포함한 PSI 변경 작업
    PsiFile psiFile = psiClass.getContainingFile();
    Project project = psiClass.getProject();
    WriteCommandAction.runWriteCommandAction(project, () -> {
      //Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

      // PSI 변경 작업 수행

      // field 위에 field 이름 주석을 달아준다.
      psiClass.addBefore(comment, field);

      // dirtyField를 target인 filed 밑에 붙여준다.
      psiClass.addAfter(dirtyFiled, field);

      PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(
          PsiDocumentManager.getInstance(project).getDocument(psiFile)
      );
    });

  }


  private void createSetterMethod(PsiClass psiClass, PsiField field) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());

    // inner class를 찾는다.
    PsiClass[] innerClasses = psiClass.getInnerClasses();

    // 마지막 class
    PsiClass lasInnerClass;

    if (innerClasses.length > 0) {

      lasInnerClass = innerClasses[innerClasses.length - 1];
    } else {
      lasInnerClass = null;
    }

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

    // Document 변경 작업을 포함한 PSI 변경 작업
    PsiFile psiFile = psiClass.getContainingFile();
    Project project = psiClass.getProject();
    //Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

    WriteCommandAction.runWriteCommandAction(project, () -> {
      PsiMethod setterMethod = elementFactory.createMethodFromText(setterText, psiClass);
      // PSI 변경 작업 수행

      if (lasInnerClass != null) {
        psiClass.addAfter(setterMethod, lasInnerClass);
      }

      if (lasInnerClass == null) {
        psiClass.add(setterMethod);
      }

      // Document 변경 작업 수행 (선택적)
//      if (document != null) {
//        document.setText(psiFile.getText());
//      }
      PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(
          PsiDocumentManager.getInstance(project).getDocument(psiFile)
      );
    });

  }

//  private void addDirtyFieldAssignment(PsiMethod setterMethod, PsiClass psiClass, PsiField field) {
//    // 이미 있는 Setter 메소드에 필드 변경 코드를 추가
//    PsiStatement[] statements = setterMethod.getBody().getStatements();
//    String fieldName = field.getName();
//    String assignmentText = "this.is" + capitalizeFirstLetter(fieldName) + "Dirty = true;";
//
//    // 필드 변경 코드가 이미 있는지 확인
//    for (PsiStatement statement : statements) {
//      if (statement.getText().equals(assignmentText)) {
//        // 이미 있는 경우 중복 추가를 피하기 위해 종료
//        return;
//      }
//    }
//
//    // 필드 변경 코드 추가
//    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
//    PsiStatement assignmentStatement = elementFactory.createStatementFromText(assignmentText,
//        psiClass);
//    setterMethod.getBody().addBefore(assignmentStatement, null);
//  }


  // 사용자 정의 ListCellRenderer
  private static class FieldListCellRenderer extends DefaultListCellRenderer {


    private static final int HORIZONTAL_PADDING = 10;
    private static final int VERTICAL_PADDING = 5;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
        boolean isSelected, boolean cellHasFocus) {
      if (value instanceof PsiField) {
        PsiField field = (PsiField) value;
        Icon dirtyIcon = IconLoader.getIcon("/icons/iLove.svg", FieldListCellRenderer.class);

        // String icon = "\uD83D\uDCCC ";
        String fieldName = field.getName();
        String fieldType = field.getType().getPresentableText();

        // 내뱉을 text앞에 dirtyIcon 붙이기
        JBLabel label = new JBLabel();
        label.setIcon(dirtyIcon);
        label.setText(fieldName + " : " + fieldType);

        label.setBorder(
            BorderFactory.createEmptyBorder(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING,
                HORIZONTAL_PADDING));

        // Set foreground color based on selection
        if (isSelected) {
          label.setForeground(list.getSelectionForeground());
          label.setBackground(list.getSelectionBackground());
        } else {
          label.setForeground(list.getForeground());
          label.setBackground(list.getBackground());
        }

//        String displayText = dirtyIcon + fieldName + " : " + fieldType;
//        return super.getListCellRendererComponent(list, label, index, isSelected,
//            cellHasFocus);
        return label;
      }
      return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
  }

}
