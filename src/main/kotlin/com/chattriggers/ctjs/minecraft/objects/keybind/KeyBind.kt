package com.chattriggers.ctjs.minecraft.objects.keybind

import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.Settings
import com.chattriggers.ctjs.triggers.RegularTrigger
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.kotlin.i18Format
import gg.essential.universal.UMinecraft
import org.apache.commons.lang3.ArrayUtils

//#if MC<=11202
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.client.registry.ClientRegistry
//#else
//$$ import com.chattriggers.ctjs.launch.mixins.transformers.KeyMappingAccessor
//$$ import net.minecraft.client.KeyMapping
//#endif

@Suppress("LeakingThis")
class KeyBind {
    private val keyBinding: KeyBinding
    private var onKeyPress: RegularTrigger? = null
    private var onKeyRelease: RegularTrigger? = null
    private var onKeyDown: RegularTrigger? = null

    private var down: Boolean = false

    init {
        KeyBindHandler.registerKeyBind(this)
    }

    /**
     * Creates a new keybind, editable in the user's controls.
     *
     * @param description what the keybind does
     * @param keyCode the keycode which the keybind will respond to, see Keyboard below. Ex. Keyboard.KEY_A
     * @param category the keybind category the keybind will be in
     * @see [org.lwjgl.input.Keyboard](http://legacy.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html)
     */
    @JvmOverloads
    constructor(description: String, keyCode: Int, category: String = "ChatTriggers") {
        //#if MC<=11202
        val possibleDuplicate = UMinecraft.getSettings().keyBindings.find {
            it.keyDescription.i18Format() == description.i18Format() && it.keyCategory.i18Format() == category.i18Format()
        }
        //#else
        //$$ val possibleDuplicate = UMinecraft.getSettings().keyMappings.find {
        //$$     it.name.i18Format() == description.i18Format() && it.category.i18Format() == category.i18Format()
        //$$ }
        //#endif

        if (possibleDuplicate != null) {
            if (possibleDuplicate in customKeyBindings) {
                keyBinding = possibleDuplicate
            } else throw IllegalArgumentException(
                "KeyBind already exists! To get a KeyBind from an existing Minecraft KeyBinding, " +
                        "use the other KeyBind constructor or Client.getKeyBindFromKey."
            )
        } else {
            //#if MC<=11202
            if (category !in KeyBinding.getKeybinds()) {
            //#else
            //$$ if (category !in KeyMappingAccessor.getCategories()) {
            //#endif
                uniqueCategories[category] = 0
            }
            uniqueCategories[category] = uniqueCategories[category]!! + 1
            keyBinding = KeyBinding(description, keyCode, category)
            //#if MC<=11202
            ClientRegistry.registerKeyBinding(keyBinding)
            //#endif
            customKeyBindings.add(keyBinding)
        }
    }

    constructor(keyBinding: KeyBinding) {
        this.keyBinding = keyBinding
    }

    fun registerKeyPress(method: Any) = run {
        onKeyPress = RegularTrigger(method, TriggerType.Other)
        onKeyPress
    }

    fun registerKeyRelease(method: Any) = run {
        onKeyRelease = RegularTrigger(method, TriggerType.Other)
        onKeyRelease
    }

    fun registerKeyDown(method: Any) = run {
        onKeyDown = RegularTrigger(method, TriggerType.Other)
        onKeyDown
    }

    internal fun onTick() {
        if (isPressed()) {
            onKeyPress?.trigger(arrayOf())
            down = true
        }

        if (isKeyDown()) {
            onKeyDown?.trigger(arrayOf())
            down = true
        }

        if (down && !isKeyDown()) {
            onKeyRelease?.trigger(arrayOf())
            down = false
        }
    }

    /**
     * Returns true if the key is pressed (used for continuous querying).
     *
     * @return whether the key is pressed
     */
    fun isKeyDown(): Boolean {
        //#if MC<=11202
        return keyBinding.isKeyDown
        //#else
        //$$ return keyBinding.isDown
        //#endif
    }

    /**
     * Returns true on the initial key press. For continuous querying use [isKeyDown].
     *
     * @return whether the key has just been pressed
     */
    fun isPressed(): Boolean {
        //#if MC<=11202
        return keyBinding.isPressed
        //#else
        //$$ return keyBinding.consumeClick()
        //#endif
    }

    /**
     * Gets the description of the key.
     *
     * @return the description
     */
    // TODO(BREAKING): Formats the description
    fun getDescription(): String {
        //#if MC<=11202
        return keyBinding.keyDescription.i18Format()
        //#else
        //$$ return keyBinding.name.i18Format()
        //#endif
    }

    /**
     * Gets the key code of the key.
     *
     * @return the integer key code
     */
    fun getKeyCode(): Int {
        //#if MC<=11202
        return keyBinding.keyCode
        //#else
        //$$ return keyBinding.key.value
        //#endif
    }

    /**
     * Gets the category of the key.
     *
     * @return the category
     */
    fun getCategory(): String {
        //#if MC<=11202
        return keyBinding.keyCategory
        //#else
        //$$ return keyBinding.category
        //#endif
    }

    /**
     * Sets the state of the key.
     *
     * @param pressed True to press, False to release
     */
    fun setState(pressed: Boolean) {
        //#if MC<=11202
        KeyBinding.setKeyBindState(keyBinding.keyCode, pressed)
        //#else
        //$$ KeyMapping.set(keyBinding.key, pressed)
        //#endif
    }

    override fun toString() = "KeyBind{" +
            "description=${getDescription()}, " +
            "keyCode=${getKeyCode()}, " +
            "category=${getCategory()}" +
            "}"

    companion object {
        private val customKeyBindings = mutableListOf<KeyBinding>()
        private val uniqueCategories = mutableMapOf<String, Int>()

        private fun removeKeyBinding(keyBinding: KeyBinding) {
            //#if MC<=11202
            Client.getMinecraft().gameSettings.keyBindings = ArrayUtils.removeElement(
                Client.getMinecraft().gameSettings.keyBindings,
                keyBinding
            )
            val category = keyBinding.keyCategory
            //#else
            //$$ UMinecraft.getSettings().keyMappings = ArrayUtils.removeElement(
            //$$     UMinecraft.getSettings().keyMappings,
            //$$     keyBinding
            //$$ )
            //$$ val category = keyBinding.category
            //#endif

            if (category in uniqueCategories) {
                uniqueCategories[category] = uniqueCategories[category]!! - 1

                if (uniqueCategories[category] == 0) {
                    uniqueCategories.remove(category)
                    //#if MC<=11202
                    KeyBinding.getKeybinds().remove(category)
                    //#else
                    //$$ KeyMappingAccessor.getCategories().remove(category)
                    //#endif
                }
            }
        }

        @JvmStatic
        fun removeKeyBind(keyBind: KeyBind) {
            val keyBinding = keyBind.keyBinding
            if (keyBinding !in customKeyBindings) return

            removeKeyBinding(keyBinding)
            customKeyBindings.remove(keyBinding)
            KeyBindHandler.unregisterKeyBind(keyBind)
        }

        @JvmStatic
        fun clearKeyBinds() {
            val copy = KeyBindHandler.getKeyBinds().toList()
            copy.forEach(::removeKeyBind)
            customKeyBindings.clear()
            KeyBindHandler.clearKeyBinds()
        }
    }
}
