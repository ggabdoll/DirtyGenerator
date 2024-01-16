package com.generator.dirty.dirtygenerator;

import static org.codehaus.plexus.util.StringUtils.capitalizeFirstLetter;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.util.PsiUtilBase;

public class Generator extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {

// 현재 열려있는 에디터의 PSI 파일을 가져옵니다.
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(e.,e.getProject())
//            .getPsiFileInEditor(e.getDataContext(), e.getProject());

        if (psiFile instanceof PsiJavaFile) {
            // Java 파일인 경우에만 처리
            PsiJavaFile javaFile = (PsiJavaFile) psiFile;
            PsiClass psiClass = javaFile.getClasses()[0]; // 첫 번째 클래스를 대상으로 가정

            // 필드들을 가져와 Setter 메서드 생성
            PsiField[] fields = psiClass.getFields();
            for (PsiField field : fields) {

            }

            Messages.showInfoMessage("Setter methods generated successfully!", "Code Generation");
        } else {
            Messages.showErrorDialog("Please open a Java file.", "Error");
        }

    // TODO: insert action logic here
  }

  private void createDirtyFiled(PsiClass psiClass, PsiField field){
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());

    String dirtyText = "@JsonIgnore \n"
        + "is" + capitalizeFirstLetter(field.getName()) + "Dirty = false";


    psiClass.add(dirtyText);
  }


  private void createSetterMethod(PsiClass psiClass, PsiField field) {
      PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());

      // Setter 메소드의 이름 및 매개변수 타입
      String setterMethodName = "set" + capitalizeFirstLetter(field.getName());
      String parameterType = field.getType().getCanonicalText();

      // 이미 같은 이름의 메소드가 있는지 확인
      PsiMethod[] existingMethods = psiClass.findMethodsByName(setterMethodName, true);
      for (PsiMethod existingMethod : existingMethods) {
          PsiParameter[] parameters = existingMethod.getParameterList().getParameters();
          if (parameters.length == 1 && parameters[0].getType().getCanonicalText().equals(parameterType)) {
              // 이미 같은 시그니처의 메소드가 있으면 필드 변경 코드를 추가
              addDirtyFieldAssignment(existingMethod, psiClass, field);
              return;
          }
      }

      // Setter 메소드 생성
      String setterText = "public void " + setterMethodName + "(" + parameterType + " " + field.getName() + ") {" +
                          "    this." + field.getName() + " = " + field.getName() + ";" +
                          "    this.is" + capitalizeFirstLetter(field.getName()) + "Dirty = true;" +
                          "}";

      PsiMethod setterMethod = elementFactory.createMethodFromText(setterText, psiClass);

      // 메소드를 클래스에 추가
      psiClass.add(setterMethod);
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
      PsiStatement assignmentStatement = elementFactory.createStatementFromText(assignmentText, psiClass);
      setterMethod.getBody().addBefore(assignmentStatement, null);
  }



}
