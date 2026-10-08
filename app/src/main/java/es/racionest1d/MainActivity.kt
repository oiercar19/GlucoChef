package es.racionest1d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import es.racionest1d.data.*
import es.racionest1d.domain.Nutrition
import es.racionest1d.domain.decimal
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { val vm: AppViewModel = viewModel(); RationsApp(vm) }
    }
}

private val teal = androidx.compose.ui.graphics.Color(0xFF176B5B)
private val pale = androidx.compose.ui.graphics.Color(0xFFE6F3EE)

@Composable private fun RationsApp(vm: AppViewModel) {
    val dark by vm.darkTheme.collectAsState(initial = false)
    val largeText by vm.largeText.collectAsState(initial = false)
    val nav = rememberNavController()
    val typography = Typography(
        bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = if (largeText) 20.sp else 16.sp, lineHeight = if (largeText) 28.sp else 23.sp),
        bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = if (largeText) 18.sp else 14.sp, lineHeight = if (largeText) 25.sp else 20.sp),
        bodySmall = androidx.compose.ui.text.TextStyle(fontSize = if (largeText) 15.sp else 12.sp, lineHeight = if (largeText) 21.sp else 17.sp),
        titleMedium = androidx.compose.ui.text.TextStyle(fontSize = if (largeText) 20.sp else 18.sp, lineHeight = if (largeText) 27.sp else 24.sp),
        titleLarge = androidx.compose.ui.text.TextStyle(fontSize = if (largeText) 24.sp else 21.sp, lineHeight = if (largeText) 31.sp else 28.sp),
        headlineMedium = androidx.compose.ui.text.TextStyle(fontSize = if (largeText) 28.sp else 25.sp, lineHeight = if (largeText) 35.sp else 32.sp)
    )
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF79D8C3),
        secondary = androidx.compose.ui.graphics.Color(0xFFA7D7C8))
        else lightColorScheme(primary = teal, secondary = androidx.compose.ui.graphics.Color(0xFF55746A),
            primaryContainer = pale, background = androidx.compose.ui.graphics.Color(0xFFF7F9F7), surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF)), typography = typography) {
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route.orEmpty()
        val tabs = listOf(
            Triple("meal/-1/false", "Comida", Icons.Filled.Add),
            Triple("ingredients", "Alimentos", Icons.Filled.Search),
            Triple("recipes", "Platos", Icons.Filled.List),
            Triple("history", "Historial", Icons.Filled.DateRange)
        )
        Scaffold(topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("GlucoChef", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { nav.navigate("settings") { launchSingleTop = true } }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }, bottomBar = {
            NavigationBar {
                tabs.forEach { (destination, label, icon) ->
                    val selected = if (destination == "meal/-1/false") route.startsWith("meal/") else route == destination
                    NavigationBarItem(selected = selected,
                        onClick = { nav.navigate(destination) { launchSingleTop = true } },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(label, maxLines = 1) })
                }
            }
        }) { inset ->
            NavHost(navController = nav, startDestination = "meal/-1/false", modifier = Modifier.padding(inset)) {
                composable("ingredients") { IngredientsScreen(vm, nav) }
                composable("ingredient/{id}") { entry -> IngredientEditor(vm, nav, entry.arguments?.getString("id")?.toLongOrNull() ?: 0) }
                composable("recipes") { RecipesScreen(vm, nav) }
                composable("recipe/{id}") { entry -> RecipeEditor(vm, nav, entry.arguments?.getString("id")?.toLongOrNull() ?: 0) }
                composable("meal/{id}/{updated}") { entry -> MealEditor(vm, nav,
                    entry.arguments?.getString("id")?.toLongOrNull() ?: -1,
                    entry.arguments?.getString("updated") == "true") }
                composable("meal-edit/{id}") { entry -> MealEditor(vm, nav,
                    entry.arguments?.getString("id")?.toLongOrNull() ?: -1, false, editing = true) }
                composable("history") { HistoryScreen(vm, nav) }
                composable("settings") { SettingsScreen(vm) }
            }
        }
    }
}

internal fun pretty(n: BigDecimal?): String = n?.setScale(2, RoundingMode.HALF_UP)?.stripTrailingZeros()?.toPlainString() ?: "—"
@Composable internal fun Totals(n: Nutrition?, title: String = "Total") {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (n == null) "Hidratos: pendientes" else "Hidratos: ${pretty(n.carbs)} g",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(if (n == null) "Raciones de HC: pendientes" else "Raciones de HC: ${pretty(n.portions)}",
                style = MaterialTheme.typography.titleMedium)
            if (n == null) Text("Falta el valor de algún alimento. Revísalo antes de usar el total.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable internal fun Page(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable internal fun InfoCard(title: String, detail: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable internal fun NumberInput(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.heightIn(min = 56.dp))
}

@Composable internal fun PrimaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable internal fun SecondaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable internal fun ErrorText(error: String?) {
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}

@Composable private fun IngredientsScreen(vm: AppViewModel, nav: NavHostController) {
    val all by vm.ingredients.collectAsState(initial = emptyList())
    var search by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Ingredient?>(null) }
    Page("Alimentos", "Busca un alimento y escribe cuánto vas a comer.") {
        OutlinedTextField(search, { search = it }, label = { Text("Nombre del alimento") },
            textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), singleLine = true)
        SecondaryAction("+ Añadir alimento nuevo", onClick = { nav.navigate("ingredient/0") })
        FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly },
            label = { Text("Solo favoritos", style = MaterialTheme.typography.bodyMedium) })
        all.filter { (!favoritesOnly || it.favorite) && (it.name.contains(search, true) || it.category.contains(search, true) || it.variant.contains(search, true)) }
            .forEach { item ->
                InfoCard("${if (item.favorite) "★ " else ""}${item.name} · ${item.variant}",
                    item.gramsPerPortion?.let { "1 ración = $it ${item.unit}" }
                        ?: item.carbsPerHundred?.let {
                            "${if (item.dataStatus == "ESTIMATED") "Orientativo · " else ""}${pretty(decimal(it))} g HC por 100 ${item.unit}"
                        } ?: "Valor pendiente") { selected = item }
            }
    }
    selected?.let { ingredient -> IngredientCalculator(vm, ingredient, onDismiss = { selected = null },
        onEdit = { selected = null; nav.navigate("ingredient/${ingredient.id}") }) }
}

@Composable private fun IngredientCalculator(vm: AppViewModel, ingredient: Ingredient, onDismiss: () -> Unit, onEdit: () -> Unit) {
    var amount by remember(ingredient.id) { mutableStateOf("100") }
    val scope = rememberCoroutineScope()
    val n = runCatching { vm.nutrition(ingredient, amount) }.getOrNull()
    DialogSurface(onDismiss) {
            Text("${ingredient.name} · ${ingredient.variant}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Pesa la parte que vas a comer: ${ingredient.weightBasis.lowercase()}.", style = MaterialTheme.typography.bodyMedium)
            NumberInput(amount, { amount = it }, "Cantidad en ${ingredient.unit}", Modifier.fillMaxWidth())
            Totals(n, "Esta cantidad")
            if (ingredient.dataStatus == "PENDING" && ingredient.notes.isNotBlank())
                Text(ingredient.notes, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            SecondaryAction(if (ingredient.custom) "Editar valor del alimento" else "Usar otro valor", onClick = onEdit)
            TextButton(onClick = { scope.launch { vm.saveIngredient(ingredient.copy(favorite = !ingredient.favorite)); onDismiss() } }) {
                Text(if (ingredient.favorite) "Quitar de favoritos" else "Guardar en favoritos")
            }
            PrimaryAction("Listo", onClick = onDismiss)
    }
}

@Composable internal fun IngredientEditor(
    vm: AppViewModel,
    nav: NavHostController,
    id: Long,
    onSaved: suspend (Long) -> Unit = { nav.popBackStack() },
    onCancel: () -> Unit = { nav.popBackStack() }
) {
    val scope = rememberCoroutineScope()
    var original by remember(id) { mutableStateOf<Ingredient?>(null) }
    var name by remember(id) { mutableStateOf("") }
    var brand by remember(id) { mutableStateOf("") }
    var category by remember(id) { mutableStateOf("Producto") }
    var variant by remember(id) { mutableStateOf("Etiqueta") }
    var state by remember(id) { mutableStateOf("") }
    var basis by remember(id) { mutableStateOf("Peso comestible") }
    var unit by remember(id) { mutableStateOf("g") }
    var perHundred by remember(id) { mutableStateOf("") }
    var perPortion by remember(id) { mutableStateOf("") }
    var packageAmount by remember(id) { mutableStateOf("") }
    var notes by remember(id) { mutableStateOf("") }
    var favorite by remember(id) { mutableStateOf(false) }
    var status by remember(id) { mutableStateOf("USER") }
    var referenceMode by remember(id) { mutableStateOf("PER100") }
    var moreDetails by remember(id) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(id) { if (id > 0) vm.getIngredient(id)?.let {
        original = it; name = it.name; brand = it.brand; category = it.category; variant = it.variant
        state = it.state; basis = it.weightBasis; unit = it.unit; perHundred = it.carbsPerHundred.orEmpty()
        perPortion = it.gramsPerPortion.orEmpty(); packageAmount = it.packageAmount; notes = it.notes; favorite = it.favorite
        status = if (it.dataStatus == "ESTIMATED" || it.dataStatus == "PENDING") it.dataStatus else "USER"
        referenceMode = if (it.gramsPerPortion != null) "RATION" else "PER100"
    } }
    Page(if (id == 0L || original?.custom == false) "Añadir alimento" else "Editar alimento",
        "Solo necesitas un nombre y el valor de hidratos.") {
        if (original?.custom == false) Text("Se guardará una copia con tu valor.", color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(name, { name = it }, label = { Text("Nombre del alimento") },
            textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp))
        Text("¿Qué dato tienes?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(selected = referenceMode == "PER100", onClick = { referenceMode = "PER100"; perPortion = "" },
                label = { Text("Tengo los hidratos por 100 g o ml", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp))
            FilterChip(selected = referenceMode == "RATION", onClick = { referenceMode = "RATION"; perHundred = "" },
                label = { Text("Sé la cantidad de una ración", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp))
        }
        if (referenceMode == "PER100") {
            Text("Usa «hidratos de carbono» de la etiqueta. No sumes los azúcares.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Medida:", style = MaterialTheme.typography.bodyLarge)
                FilterChip(selected = unit == "g", onClick = { unit = "g" }, label = { Text("100 g") })
                FilterChip(selected = unit == "ml", onClick = { unit = "ml" }, label = { Text("100 ml") })
            }
            NumberInput(perHundred, { perHundred = it }, "Hidratos por 100 $unit", Modifier.fillMaxWidth())
        } else {
            NumberInput(perPortion, { perPortion = it }, "${unit} que equivalen a 1 ración", Modifier.fillMaxWidth())
        }
        TextButton(onClick = { moreDetails = !moreDetails }) {
            Text(if (moreDetails) "Ocultar detalles" else "Más detalles (opcional)", style = MaterialTheme.typography.bodyLarge)
        }
        if (moreDetails) {
            OutlinedTextField(brand, { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(variant, { variant = it }, label = { Text("Variante: crudo, cocido…") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(basis, { basis = it }, label = { Text("Peso: comestible o escurrido") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state, { state = it }, label = { Text("Estado al pesar") }, modifier = Modifier.fillMaxWidth())
            NumberInput(packageAmount, { packageAmount = it }, "Contenido del envase", Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
            Text("Calidad del dato", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = status == "USER", onClick = { status = "USER" }, label = { Text("Dato de etiqueta", style = MaterialTheme.typography.bodyMedium) }, modifier = Modifier.fillMaxWidth())
                FilterChip(selected = status == "ESTIMATED", onClick = { status = "ESTIMATED" }, label = { Text("Valor estimado", style = MaterialTheme.typography.bodyMedium) }, modifier = Modifier.fillMaxWidth())
                FilterChip(selected = status == "PENDING", onClick = { status = "PENDING" }, label = { Text("Pendiente de comprobar", style = MaterialTheme.typography.bodyMedium) }, modifier = Modifier.fillMaxWidth())
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(favorite, { favorite = it }); Text("Favorito", style = MaterialTheme.typography.bodyLarge) }
        }
        ErrorText(error)
        PrimaryAction("Guardar alimento", onClick = { scope.launch { runCatching {
            require(name.isNotBlank()) { "Escribe el nombre" }
            require(status == "PENDING" || perHundred.isNotBlank() || perPortion.isNotBlank()) { "Indica una equivalencia nutricional" }
            val newId = if (original?.custom == true) id else 0L
            vm.saveIngredient(Ingredient(id = newId, name = name.trim(), brand = brand.trim(), category = category,
                variant = variant, state = state, weightBasis = basis, unit = unit,
                gramsPerPortion = perPortion.takeIf { it.isNotBlank() }, carbsPerHundred = perHundred.takeIf { it.isNotBlank() },
                dataStatus = status, source = when (status) {
                    "PENDING" -> "Pendiente de comprobar"
                    "ESTIMATED" -> "Estimación del usuario"
                    else -> if (brand.isBlank()) "Dato del usuario" else "Etiqueta: $brand"
                },
                sourceDate = java.time.LocalDate.now().toString(), packageAmount = packageAmount, notes = notes,
                favorite = favorite, custom = true))
        }.onSuccess { onSaved(it) }.onFailure { error = it.message } } })
        SecondaryAction("Volver sin guardar", onClick = onCancel)
    }
}

