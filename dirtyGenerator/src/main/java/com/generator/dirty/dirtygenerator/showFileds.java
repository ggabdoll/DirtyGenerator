package com.generator.dirty.dirtygenerator;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

public class showFileds {

  private JButton generateButton;
  private JButton cancle;
public showFileds() {
  cancle.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
      super.mouseClicked(e);
    }
  });
}
}
