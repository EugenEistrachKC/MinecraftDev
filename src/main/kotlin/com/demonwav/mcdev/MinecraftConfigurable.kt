/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev

import com.demonwav.mcdev.update.ConfigurePluginUpdatesDialog
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.*

class MinecraftConfigurable : Configurable {

    private lateinit var panel: JPanel
    private lateinit var showProjectPlatformIconsCheckBox: JCheckBox
    private lateinit var showEventListenerGutterCheckBox: JCheckBox
    private lateinit var showChatColorUnderlinesCheckBox: JCheckBox
    private lateinit var chatColorUnderlinesComboBox: JComboBox<MinecraftSettings.UnderlineType>
    private lateinit var showChatGutterIconsCheckBox: JCheckBox
    private lateinit var changePluginUpdateChannelButton: JButton
    private lateinit var useCustomVersionUrl: JCheckBox
    private lateinit var customVersionUrl: JTextField

    @Nls
    override fun getDisplayName() = "Minecraft Development"
    override fun getHelpTopic(): String? = null

    override fun createComponent(): JComponent {
        showChatColorUnderlinesCheckBox.addActionListener { setUnderlineBox() }

        return panel
    }

    private fun init() {
        for (type in MinecraftSettings.UnderlineType.values()) {
            chatColorUnderlinesComboBox.addItem(type)
        }

        val settings = MinecraftSettings.instance

        showProjectPlatformIconsCheckBox.isSelected = settings.isShowProjectPlatformIcons
        showEventListenerGutterCheckBox.isSelected = settings.isShowEventListenerGutterIcons
        showChatGutterIconsCheckBox.isSelected = settings.isShowChatColorGutterIcons
        showChatColorUnderlinesCheckBox.isSelected = settings.isShowChatColorUnderlines

        chatColorUnderlinesComboBox.selectedIndex = settings.underlineTypeIndex
        setUnderlineBox()

        changePluginUpdateChannelButton.addActionListener { ConfigurePluginUpdatesDialog().show() }

        useCustomVersionUrl.isSelected = settings.useCustomVersionUrl
        customVersionUrl.text = settings.customVersionUrl
    }

    private fun setUnderlineBox() {
        chatColorUnderlinesComboBox.isEnabled = showChatColorUnderlinesCheckBox.isSelected
    }

    override fun isModified(): Boolean {
        val settings = MinecraftSettings.instance

        return showProjectPlatformIconsCheckBox.isSelected != settings.isShowProjectPlatformIcons ||
            showEventListenerGutterCheckBox.isSelected != settings.isShowEventListenerGutterIcons ||
            showChatGutterIconsCheckBox.isSelected != settings.isShowChatColorGutterIcons ||
            showChatColorUnderlinesCheckBox.isSelected != settings.isShowChatColorUnderlines ||
            chatColorUnderlinesComboBox.selectedItem !== settings.underlineType ||
            useCustomVersionUrl.isSelected != settings.useCustomVersionUrl ||
            customVersionUrl.text != settings.customVersionUrl
    }

    override fun apply() {
        val settings = MinecraftSettings.instance

        settings.isShowProjectPlatformIcons = showProjectPlatformIconsCheckBox.isSelected
        settings.isShowEventListenerGutterIcons = showEventListenerGutterCheckBox.isSelected
        settings.isShowChatColorGutterIcons = showChatGutterIconsCheckBox.isSelected
        settings.isShowChatColorUnderlines = showChatColorUnderlinesCheckBox.isSelected
        settings.underlineType = chatColorUnderlinesComboBox.selectedItem as MinecraftSettings.UnderlineType
        settings.useCustomVersionUrl = useCustomVersionUrl.isSelected
        settings.customVersionUrl = customVersionUrl.text
    }

    override fun reset() {
        init()
    }
}
