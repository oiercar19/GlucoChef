package es.racionest1d

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.racionest1d.data.*
import es.racionest1d.domain.Nutrition
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Application.settings by preferencesDataStore("settings")

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.open(application)
    private val repository = RationsRepository(database)
    val ingredients = repository.ingredients
    val recipes = repository.recipes
    val recipeSummaries = repository.recipeSummaries
    val meals = repository.meals
    private val darkKey = booleanPreferencesKey("dark_theme")
    private val largeTextKey = booleanPreferencesKey("large_text")
    val darkTheme = application.settings.data.map { it[darkKey] ?: false }
    val largeText = application.settings.data.map { it[largeTextKey] ?: false }

    init { viewModelScope.launch { repository.syncCatalog() } }

    fun setDarkTheme(value: Boolean) = viewModelScope.launch {
        getApplication<Application>().settings.edit { it[darkKey] = value }
    }
    fun setLargeText(value: Boolean) = viewModelScope.launch {
        getApplication<Application>().settings.edit { it[largeTextKey] = value }
    }

    fun nutrition(ingredient: Ingredient, amount: String): Nutrition? = repository.nutrition(ingredient, amount)
    suspend fun getIngredient(id: Long) = repository.getIngredient(id)
    suspend fun saveIngredient(ingredient: Ingredient) = repository.saveIngredient(ingredient)
    suspend fun getRecipe(id: Long) = repository.getRecipe(id)
    suspend fun saveRecipe(recipe: Recipe, lines: List<DraftIngredient>) = repository.saveRecipe(recipe, lines)
    suspend fun duplicateRecipe(id: Long) = repository.duplicateRecipe(id)
    suspend fun deleteRecipe(id: Long) = repository.deleteRecipe(id)
    suspend fun getMeal(id: Long) = repository.getMeal(id)
    suspend fun mealLine(kind: String, sourceId: Long, mode: String, amount: String, latestIngredients: Boolean = false) =
        repository.mealLine(kind, sourceId, mode, amount, latestIngredients)
    suspend fun saveMeal(title: String, notes: String, template: Boolean, lines: List<DraftMealItem>) =
        repository.saveMeal(title, notes, template, lines)
    suspend fun repeatMeal(id: Long, updated: Boolean) = repository.repeatMeal(id, updated)
    suspend fun deleteMeal(id: Long) = repository.deleteMeal(id)

    override fun onCleared() {
        database.close()
        super.onCleared()
    }
}
