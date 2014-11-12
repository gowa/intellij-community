/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ui.components;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Consumer;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * @author Irina.Chernushina on 11/12/2014.
 */
public class SliderSelectorAction extends DumbAwareAction {
  @NotNull private final Configuration myConfiguration;

  public SliderSelectorAction(@Nullable String text, @Nullable String description, @Nullable Icon icon,
                              @NotNull Configuration configuration) {
    super(text, description, icon);
    myConfiguration = configuration;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    final String tooltip = myConfiguration.getTooltip();
    if (tooltip != null) {
      e.getPresentation().setText(getTemplatePresentation().getText() + " (" + tooltip + ")");
      e.getPresentation().setDescription(getTemplatePresentation().getDescription() + " (" + tooltip + ")");
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final JPanel result = new JPanel(new BorderLayout());
    final JLabel label = new JLabel(myConfiguration.getSelectText());
    label.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(label, BorderLayout.NORTH);
    result.add(wrapper, BorderLayout.WEST);

    final JSlider slider = new JSlider(SwingConstants.HORIZONTAL, myConfiguration.getMin(), myConfiguration.getMax(), myConfiguration.getSelected());
    slider.setMinorTickSpacing(1);
    slider.setPaintTicks(true);
    slider.setPaintTrack(true);
    slider.setSnapToTicks(true);
    UIUtil.setSliderIsFilled(slider, true);
    slider.setPaintLabels(true);
    slider.setLabelTable(myConfiguration.getDictionary());
    result.add(slider, BorderLayout.CENTER);
    final Runnable[] closeMe = new Runnable[1];
    if (myConfiguration.isShowOk()) {
      final JButton done = new JButton("Done");
      result.add(SwingHelper.wrapWithoutStretch(done), BorderLayout.SOUTH);
      done.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (closeMe[0] != null) closeMe[0].run();
        }
      });
    }

    final JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(result, slider).createPopup();
    final Runnable finalRunnable = new Runnable() {
      @Override
      public void run() {
        int value = slider.getModel().getValue();
        myConfiguration.getResultConsumer().consume(value);
      }
    };
    closeMe[0] = new Runnable() {
      @Override
      public void run() {
        finalRunnable.run();
        popup.closeOk(null);
      }
    };
    popup.setFinalRunnable(finalRunnable);
    InputEvent inputEvent = e.getInputEvent();
    if (inputEvent instanceof MouseEvent) {
      int width = result.getPreferredSize().width;
      MouseEvent inputEvent1 = (MouseEvent)inputEvent;
      Point point1 = new Point(inputEvent1.getX() - width / 2, inputEvent1.getY());
      RelativePoint point = new RelativePoint(inputEvent1.getComponent(), point1);
      popup.show(point);
    } else {
      popup.showInBestPositionFor(e.getDataContext());
    }
  }

  public static class Configuration {
    @NotNull
    private final String mySelectText;
    @NotNull
    private final Dictionary myDictionary;
    private final int mySelected;
    private final int myMin;
    private final int myMax;
    @NotNull
    private final Consumer<Integer> myResultConsumer;
    private boolean showOk = false;

    public Configuration(int selected, @NotNull Dictionary dictionary, @NotNull String selectText, @NotNull Consumer<Integer> consumer) {
      mySelected = selected;
      myDictionary = new Hashtable<Integer, JComponent>();
      mySelectText = selectText;
      myResultConsumer = consumer;

      int min = 1;
      int max = 0;
      final Enumeration keys = dictionary.keys();
      while (keys.hasMoreElements()) {
        final Integer key = (Integer)keys.nextElement();
        final String value = (String)dictionary.get(key);
        myDictionary.put(key, markLabel(value));
        min = Math.min(min, key);
        max = Math.max(max, key);
      }
      myMin = min;
      myMax = max;
    }

    private static JLabel markLabel(final String text) {
      JLabel label = new JLabel(text);
      label.setFont(UIUtil.getLabelFont());
      return label;
    }

    @NotNull
    public String getSelectText() {
      return mySelectText;
    }

    @NotNull
    public Dictionary getDictionary() {
      return myDictionary;
    }

    @NotNull
    public Consumer<Integer> getResultConsumer() {
      return myResultConsumer;
    }

    public int getSelected() {
      return mySelected;
    }

    public int getMin() {
      return myMin;
    }

    public int getMax() {
      return myMax;
    }

    public boolean isShowOk() {
      return showOk;
    }

    public void setShowOk(boolean showOk) {
      this.showOk = showOk;
    }

    public String getTooltip() {
      return null;
    }
  }
}
