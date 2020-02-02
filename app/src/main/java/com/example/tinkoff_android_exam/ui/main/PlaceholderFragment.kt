package com.example.tinkoff_android_exam.ui.main

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.tinkoff_android_exam.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.lang.reflect.AccessibleObject.setAccessible
import android.widget.Spinner
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.example.tinkoff_android_exam.SharedPrefsCache
import kotlin.Exception
import android.content.SharedPreferences
import kotlinx.android.synthetic.main.fragment_main.*


/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    private val APIKEY = "4db6732e5805b733b434"

//    блокировка изменения поля с конвертируемы значением для второго задания
    private var changeLock = false

//    индексы выбранных в данный момент валют
    private var to_curr_selected = -1
    private var from_curr_selected = -1

//    флаг наличия интернет соединения
    private var isOnline = false

//    самописный класс реализующий сохраниене кеша в SharedPreferences
    private lateinit var cacher : SharedPrefsCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }

        cacher = SharedPrefsCache(this.activity!!.getSharedPreferences("pref", Context.MODE_PRIVATE))
    }

//    метод возвращающий состояние интернет соединения
    fun checkConnection(){
        val cm = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        isOnline = activeNetwork?.isConnectedOrConnecting == true
    }

//    метод осуществляющий GET запрос к АПИ
    fun askAPI(url: String) : JSONObject{
        try {
//            проверка интернет соединения для избечания ошибок во время запроса
//            все остальные ошибки считаются ошибками на стороне АПИ
            checkConnection()
            if (isOnline) {
                var res = ""

//                для отправки запроса используется отдельный поток
                val thrd = Thread({
                    res = URL(url).readText()
                })

//                после запуска, поток включается в основной, чтобы была возможность дождаться его выполнения
                thrd.start()
                thrd.join()

                return JSONObject(res)
            } else {
                Toast.makeText(context, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                return JSONObject()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "К сожалению АПИ не отвечает. Повторите попытку позже.", Toast.LENGTH_LONG).show()
            return JSONObject()
        }
    }

//    метод конвертирующий одну валюту в другую
//    проверяет необходимые значения и формирует запрос к АПИ
//    при нормальных обстоятельствах возвращается значение конвертированной валюты
//    при возникновении ошибки возвращается -1
    fun convertCurrency(inpt : String, api_query : String) : Double{
//        проверка наличия в строке исключительно числовых значений
        if (inpt.matches("-?\\d+(\\.\\d+)?".toRegex())) {

            var convert_value = inpt.toDouble()
//            запрос данных из АПИ
            val res = askAPI("https://free.currconv.com/api/v7/convert?apiKey=$APIKEY&q=$api_query&compact=ultra")

            if (res.has(api_query)) {
//                округление результата до 5 знаков
                return Math.round((convert_value * res.getDouble(api_query))*100000.0) / 100000.0
            } else {
                return -1.0
            }
        } else if (inpt == ""){
            return -1.0
        } else {
            Toast.makeText(context, "Вы ввели некорректное значение", Toast.LENGTH_LONG).show()
            return -1.0
        }
    }

//    основное задание
//    метод проверяющий наличие необходимых для конвертации данных и интернет соединения
//    при отсутствии интернет соединения, проверяется наличия данных в кеше
    fun buttonConvert(root: View){
        val to_value_noneditable: TextView = root.findViewById(R.id.to_value_noneditable)
        val from_value: EditText = root.findViewById(R.id.from_value)
        val convert_button: Button = root.findViewById(R.id.convert_button)
        val from_currencies: Spinner = root.findViewById(R.id.from_currencies)
        val to_currencies: Spinner = root.findViewById(R.id.to_currencies)

//        проверка напонения списка доступных валют
        if (!from_currencies.isEmpty()) {
//            progressBar.visibility = View.VISIBLE
//                формирование токена запроса в АПИ
            val api_query = "${from_currencies.selectedItem}_${to_currencies.selectedItem}"

            var result = -1.0
            checkConnection()
//            при наличии интернет соедиения, значение запрашивается в АПИ.
//            при отсутствии интернет соединения, значение проверяется в кеше
//            при отсутствии и интернет соединения и данных в кеше, возвращается -1
//            приоритет у запроса из АПИ, так как курсы валют меняются со временем и в кеше может быть устаревшая информация
            if (isOnline) {
                result = convertCurrency(from_value.text.toString(), api_query)
                cacher.set(api_query,result.toString())
            }
            if (cacher.has(api_query) && (!isOnline || result == -1.0)) {
                result = cacher.get(api_query)!!.toDouble()
            } else if (!cacher.has(api_query) && !isOnline && result == -1.0) {
                Toast.makeText(context, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
            }

            if (result >= 0.0) {
                to_value_noneditable.setText(result.toString())
            } else {
                to_value_noneditable.setText("")
            }
//            progressBar.visibility = View.INVISIBLE
        } else {
//            если список валют не наполнен, предпринимается попытка переподключения к АПИ
            checkConnection()
            if (isOnline) {
                fillCurrencies(root)
                convert_button.text = "Конвертировать"
            } else {
                Toast.makeText(context, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                convert_button.text = "Переподключиться"
            }
        }
    }

//    дополнительное задание
//    метод конвертирующий значения на ходу, при изменениии одного из полей со значениями для конвертации
    fun onlineConvert(root: View, source : EditText, dest : EditText, toUp : Boolean) {
        val from_currencies: Spinner = root.findViewById(R.id.from_currencies)
        val to_currencies: Spinner = root.findViewById(R.id.to_currencies)
//        проверка что текущее изменение является изменением пользователя, а не программным
        if (!changeLock) {
            if (!from_currencies.isEmpty()) {
//                progressBar.visibility = View.VISIBLE
//                формирование токена запроса в АПИ
                var api_query = ""
                if (toUp) {
                    api_query = "${to_currencies.selectedItem}_${from_currencies.selectedItem}"
                } else {
                    api_query = "${from_currencies.selectedItem}_${to_currencies.selectedItem}"
                }

                var result = -1.0

//            при наличии интернет соедиения, значение запрашивается в АПИ.
//            при отсутствии интернет соединения, значение проверяется в кеше
//            при отсутствии и интернет соединения и данных в кеше, возвращается -1
//            приоритет у запроса из АПИ, так как курсы валют меняются со временем и в кеше может быть устаревшая информация
                checkConnection()
                if (isOnline) {
                    result = convertCurrency(source.text.toString(), api_query)
                    cacher.set(api_query,result.toString())
                }
                if (cacher.has(api_query) && (!isOnline || result == -1.0)) {
                    result = cacher.get(api_query)!!.toDouble()
                } else if (!cacher.has(api_query) && !isOnline && result == -1.0) {
                    Toast.makeText(context, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                }

//                захват блокировки, для того чтобы при изменении значения в другом поле не возникла бесконечная петля
                changeLock = true
                if (result >= 0.0) {
                    dest.setText(result.toString())
                } else {
                    dest.setText("")
                }
//                освобождение блокировки
                changeLock = false
//                progressBar.visibility = View.INVISIBLE
            } else {
//            если список валют не наполнен, предпринимается попытка переподключения к АПИ
                checkConnection()
                if (isOnline) {
                    fillCurrencies(root)
                } else {
                    Toast.makeText(context, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

//    мето получения списка всех валют из АПИ
    fun fillCurrencies(root : View){
        val from_currencies: Spinner = root.findViewById(R.id.from_currencies)
        val to_currencies: Spinner = root.findViewById(R.id.to_currencies)

        val to_value: EditText = root.findViewById(R.id.to_value)
        val from_value: EditText = root.findViewById(R.id.from_value)

        var currencies = JSONObject()

//            при наличии интернет соедиения, значение запрашивается в АПИ.
//            при отсутствии интернет соединения, значение проверяется в кеше
//            при отсутствии и интернет соединения и данных в кеше, возвращается пустой список
//            приоритет у проверки в кеше, так как валюты не меняются со временем и в кеше должна находится достоверная информация
        checkConnection()
        if (cacher.has("currencies")) {
            currencies = JSONObject(cacher.get("currencies"))
        } else if (isOnline) {
            currencies = askAPI("https://free.currconv.com/api/v7/currencies?apiKey=$APIKEY")
            cacher.set("currencies", currencies.toString())
        } else {
            Toast.makeText(context, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
        }

//        проверка валидного ответа от АПИ
        if (currencies.has("results")){
            currencies=currencies.getJSONObject("results")

//            напоняется список из сокращенных названий валют для созадния даптера для объекта Spinner
            var currencies_list = mutableListOf<String>()
            for (i in currencies.keys()){
                currencies_list .add(currencies.getJSONObject(i).getString("id"))
            }
//            список сортируется в алфавитном порядке для удобства использования
            currencies_list.sort()

//            задаются начальные положения спинеров
            to_curr_selected = currencies_list.indexOf("USB")
            from_curr_selected = currencies_list.indexOf("RUB")

//            описывается адаптер для каждого из спинеров
            val from_adapter = ArrayAdapter(context!!, R.layout.spinner_custom_item, currencies_list )
            from_adapter.setDropDownViewResource(R.layout.spinner_custom_item)
            from_currencies.adapter = from_adapter
            from_currencies.setSelection(from_curr_selected)
//            в метод описывающий поведение при изменении текщуего значения встроена логика запрящающая выбирать одну валюту в двух спинерах
            from_currencies.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val item = from_adapter.getItem(position)
                    if (item == to_currencies.selectedItem){
                        to_currencies.setSelection(from_curr_selected)
                    }
//                    сохранение текущей выбранной валюты
                    from_curr_selected = position
//                    запускается пересчет значения при изменении валюты
                    buttonConvert(root)
                    onlineConvert(root, to_value, from_value, true)
                }
            }

//            аналогично предыдущему фрагменту
            val to_adapter = ArrayAdapter(context!!, R.layout.spinner_custom_item, currencies_list)
            to_adapter.setDropDownViewResource(R.layout.spinner_custom_item)
            to_currencies.adapter = to_adapter
            to_currencies.setSelection(to_curr_selected)
            to_currencies.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val item = to_adapter.getItem(position)
                    if (item == from_currencies.selectedItem){
                        from_currencies.setSelection(to_curr_selected)
                    }
                    to_curr_selected = position
                    buttonConvert(root)
                    onlineConvert(root, from_value, to_value, false)
                }
            }
        } else {
            Toast.makeText(context, "К сожалению АПИ не отвечает. Повторите попытку позже.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        checkConnection()
//        уведомление пользователя об отсутствии интернет соединения
        if (!isOnline) {
            Toast.makeText(context, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
        }

        val root = inflater.inflate(R.layout.fragment_main, container, false)

        val imageView: ImageView = root.findViewById(R.id.imageView)
        val to_value: EditText = root.findViewById(R.id.to_value)
        val to_value_noneditable: TextView = root.findViewById(R.id.to_value_noneditable)
        val from_value: EditText = root.findViewById(R.id.from_value)
        val convert_button: Button = root.findViewById(R.id.convert_button)

        pageViewModel.index.observe(this, Observer<Int> {
//        задаем направление стрелок на изображении
            imageView.setImageResource(if (it == 1) R.drawable.arrow_down else R.drawable.arrow_up)
//            запрещаем редактирование целевого значения для первого варианта решения
            to_value.visibility = if (it == 1) View.INVISIBLE else View.VISIBLE
            to_value_noneditable.visibility = if (it == 1) View.VISIBLE else View.INVISIBLE
//            скрываем кнопку для второго варианта решения
            convert_button.visibility = if (it == 1) View.VISIBLE else View.INVISIBLE
        })

//        изменение надписи на кнопке при отсутствии интренет соединения
        convert_button.text = if (isOnline) "Конвертировать" else "Переподключиться"

//        наполнение спинеров данными о валютах
        fillCurrencies(root)

//        основное задание
//        опеределение поведения кнопки при нажатии
        convert_button.setOnClickListener {
            buttonConvert(root)
        }

//        дополнительное задание
//        оперделение поведения текстовых полей при изменении значений в них
        from_value.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
//                отключение логики онлайн конвертации, если активен первый экран
                if (to_value.isVisible) {
                    onlineConvert(root, from_value, to_value, false)
                }
            }
        }
        )

        to_value.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                onlineConvert(root, to_value, from_value, true)
            }
        }
        )

        return root
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}