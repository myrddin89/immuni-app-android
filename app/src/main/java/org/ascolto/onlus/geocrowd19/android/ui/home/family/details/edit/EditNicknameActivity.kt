package org.ascolto.onlus.geocrowd19.android.ui.home.family.details.edit

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.bendingspoons.base.extensions.*
import com.bendingspoons.base.utils.ScreenUtils
import kotlinx.android.synthetic.main.user_edit_nickname_activity.*
import kotlinx.android.synthetic.main.user_edit_nickname_activity.radioGroup
import kotlinx.android.synthetic.main.user_edit_nickname_activity.textField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ascolto.onlus.geocrowd19.android.AscoltoActivity
import org.ascolto.onlus.geocrowd19.android.R
import org.ascolto.onlus.geocrowd19.android.db.entity.Gender
import org.ascolto.onlus.geocrowd19.android.loading
import org.ascolto.onlus.geocrowd19.android.models.Nickname
import org.ascolto.onlus.geocrowd19.android.models.NicknameType
import org.ascolto.onlus.geocrowd19.android.models.User
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf

class EditNicknameActivity : AscoltoActivity(), CompoundButton.OnCheckedChangeListener {
    private lateinit var viewModel: EditDetailsViewModel
    private lateinit var userId: String
    val items = LinkedHashMap<Pair<NicknameType, Gender>, RadioButton>()

    override fun onPause() {
        super.onPause()
        textField?.hideKeyboard()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_edit_nickname_activity)
        setLightStatusBarFullscreen(resources.getColor(android.R.color.transparent))
        userId = intent?.extras?.getString("userId")!!
        viewModel = getViewModel { parametersOf(userId)}

        viewModel.navigateBack.observe(this, Observer {
            it.getContentIfNotHandled()?.let {
                finish()
            }
        })

        viewModel.user.observe(this, Observer {
            buildWidget(it)
            items.values.forEach { it.isChecked = false}
            items[Pair(it.nickname?.type, it.gender)]?.isChecked = true
            if(it.nickname?.type == NicknameType.OTHER) {
                textField.setText(it.nickname.value ?: "")
                textField.setSelection(textField.text.length)
                editTextGroup.visible()
            } else {
                editTextGroup.gone()
            }
        })

        viewModel.loading.observe(this, Observer {
            loading(it)
        })

        back.setOnClickListener { finish() }
        textField.doOnTextChanged { text, _, _, _ ->
            validate()
        }
        update.setOnClickListener {
            var nickname: Nickname? = null

            items.keys.forEach { pair ->
                val radio = items[pair]
                if(radio?.isChecked == true) {
                    nickname = if(pair.first == NicknameType.OTHER) {
                        Nickname(NicknameType.OTHER, textField.text.toString().trim())
                    } else {
                        Nickname(pair.first)
                    }
                }
            }

            val user = viewModel.user()
            user?.let {
                viewModel.updateUser(user.copy(nickname = nickname))
            }
        }

        textField.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                GlobalScope.launch(Dispatchers.Main) {
                    delay(500)
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }

            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        var selectedType: NicknameType? = null
        if (isChecked) {
            items.keys.forEach { key ->
                if (items[key] == buttonView) {
                    selectedType = key.first
                }
            }
            if (selectedType == NicknameType.OTHER) {
                editTextGroup.visible()
                textField.showKeyboard()
            }
            else {
                editTextGroup.gone()
                textField.hideKeyboard()
            }
        }

        validate()
    }

    private fun validate() {
        var selectedType: NicknameType? = null
        items.keys.forEach { pair ->
            val radio = items[pair]
            if(radio?.isChecked == true) {
                selectedType = pair.first
            }
        }

        var valid = selectedType != null

        if (selectedType == NicknameType.OTHER) {
            valid = valid && textField.text.toString().isNotEmpty() && textField.text.toString().length <= 5
        }

        update.isEnabled = valid
    }

    private fun buildWidget(user: User) {
        var cont = 0
        val mainUser = viewModel.mainUser()

        enumValues<NicknameType>().forEach { type ->
            val ageGroup = user.ageGroup
            val gender = user.gender

            if (type == NicknameType.OTHER) {
            } // at the end
            else if (ageGroup < mainUser.ageGroup && type in setOf(
                    NicknameType.PARENT,
                    NicknameType.MATERNAL_GRANDPARENT,
                    NicknameType.PATERNAL_GRANDPARENT
                )) {
            } // skip
            else if (ageGroup > mainUser.ageGroup && type in setOf(
                    NicknameType.CHILD_1,
                    NicknameType.CHILD_2,
                    NicknameType.CHILD_3,
                    NicknameType.CHILD_4
                )) {
            } // skip
            else {
                items.apply {
                    put(
                        Pair(type, gender),
                        RadioButton(applicationContext).apply {
                            id = cont++
                            tag = cont
                            text = Nickname(type, "").humanReadable(context, gender)
                            val tf = ResourcesCompat.getFont(context, R.font.euclid_circular_bold)
                            typeface = tf
                            textSize = 18f
                            buttonDrawable =
                                ContextCompat.getDrawable(context, R.drawable.radio_button)
                            val paddingLeft = ScreenUtils.convertDpToPixels(context, 20)
                            val paddingTop = ScreenUtils.convertDpToPixels(context, 16)
                            val paddingBottom = ScreenUtils.convertDpToPixels(context, 16)
                            setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
                            setTextColor(Color.parseColor("#495D74"))
                            setOnCheckedChangeListener(this@EditNicknameActivity)
                        }
                    )
                }
            }
        }

        // place OTHERS at the end
        items.apply {
            put(
                Pair(NicknameType.OTHER, Gender.FEMALE),
                RadioButton(applicationContext).apply {
                    id = cont++
                    tag = cont
                    text = context.getString(R.string.choose_a_nickname)
                    val tf = ResourcesCompat.getFont(context, R.font.euclid_circular_bold)
                    typeface = tf
                    textSize = 18f
                    buttonDrawable = ContextCompat.getDrawable(context, R.drawable.radio_button)
                    val paddingLeft = ScreenUtils.convertDpToPixels(context, 20)
                    val paddingTop = ScreenUtils.convertDpToPixels(context, 16)
                    val paddingBottom = ScreenUtils.convertDpToPixels(context, 16)
                    setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
                    setTextColor(Color.parseColor("#495D74"))
                    setOnCheckedChangeListener(this@EditNicknameActivity)
                })
        }

        radioGroup.apply {
            items.values.forEach { addView(it) }
        }
    }
}
