package com.generator.dirty.dirtygenerator;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import org.jetbrains.annotations.Nullable;

public class FieldSelectionDialog extends DialogWrapper {

  private List<PsiField> selectedFields;

  public FieldSelectionDialog(Project project, PsiClass psiClass) {
    super(project);
    setTitle("Select Fields for Dirty Generation");
    init();
    initFields(psiClass);
  }

  private void initFields(PsiClass psiClass) {
    selectedFields = new ArrayList<>();

    JPanel panel = new JPanel(new BorderLayout());

    JLabel classNameLabel = new JLabel("Class: " + psiClass.getQualifiedName());
    panel.add(classNameLabel, BorderLayout.NORTH);

    DefaultListModel<PsiField> listModel = new DefaultListModel<>();
    Arrays.stream(psiClass.getFields())
        .filter(field -> !field.hasModifierProperty("static"))
        .forEach(listModel::addElement);

    JBList<PsiField> fieldList = new JBList<>(listModel);
    fieldList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    // 사용자 정의된 ListCellRenderer를 사용하여 필드 정보를 보여주기
    fieldList.setCellRenderer(new FieldListCellRenderer());

    JBScrollPane scrollPane = new JBScrollPane(fieldList);
    panel.add(scrollPane, BorderLayout.CENTER);

    // filed를 panel에 보여준다.
    getContentPane().add(panel, BorderLayout.CENTER);

    // 창 크기
    setSize(400, 500);

  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return null; // We set the content in initFields
  }

  @Override
  protected void doOKAction() {
    selectedFields = Arrays.asList(
        ((JList<PsiField>) getContentPane().getComponent(0)).getSelectedValuesList()
            .toArray(new PsiField[0]));
    super.doOKAction();
  }

  public List<PsiField> getSelectedFields() {
    return selectedFields;
  }


  // 사용자 정의 ListCellRenderer
  private static class FieldListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
        boolean isSelected, boolean cellHasFocus) {
      if (value instanceof PsiField) {
        PsiField field = (PsiField) value;
//        String icon = "\uD83D\uDCCC ";
        String fieldName = field.getName();
        String fieldType = field.getType().getPresentableText();
        String displayText = fieldName + " : " + fieldType;
        return super.getListCellRendererComponent(list, displayText, index, isSelected,
            cellHasFocus);
      }
      return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
  }
}


