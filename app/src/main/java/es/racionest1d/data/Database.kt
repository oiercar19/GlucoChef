package es.racionest1d.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val variant: String,
    val state: String,
    val weightBasis: String,
    val unit: String = "g",
    val gramsPerPortion: String? = null,
    val carbsPerHundred: String? = null,
    val dataStatus: String = "VERIFIED",
    val source: String,
    val sourceUrl: String = "",
    val sourceDate: String,
    val sourceVersion: String = "",
    val brand: String = "",
    val packageAmount: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val custom: Boolean = false
)

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Otros",
    val portions: String,
    val finishedWeight: String? = null,
    val manualCarbRations: String? = null,
    val notes: String = "",
    val photoUri: String? = null,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0
)

@Entity(tableName = "recipe_ingredients", foreignKeys = [
    ForeignKey(entity = Recipe::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE)
], indices = [Index("recipeId")])
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val ingredientId: Long?,
    val nameSnapshot: String,
    val variantSnapshot: String,
    val amount: String,
    val unit: String,
    val gramsPerPortionSnapshot: String?,
    val carbsPerHundredSnapshot: String?,
    val carbsSnapshot: String?,
    val statusSnapshot: String,
    val sourceSnapshot: String
)

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val insulin: String = "",
    val manualCarbRations: String? = null,
    val isTemplate: Boolean = false,
    val carbsSnapshot: String?
)

@Entity(tableName = "meal_items", foreignKeys = [
    ForeignKey(entity = Meal::class, parentColumns = ["id"], childColumns = ["mealId"], onDelete = ForeignKey.CASCADE)
], indices = [Index("mealId")])
data class MealItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val kind: String,
    val sourceId: Long?,
    val nameSnapshot: String,
    val mode: String,
    val amount: String,
    val unit: String,
    val carbsSnapshot: String?,
    val sourceSnapshot: String
)

data class RecipeWithIngredients(
    @Embedded val recipe: Recipe,
    @Relation(parentColumn = "id", entityColumn = "recipeId") val ingredients: List<RecipeIngredient>
)

data class MealWithItems(
    @Embedded val meal: Meal,
    @Relation(parentColumn = "id", entityColumn = "mealId") val items: List<MealItem>
)

@Dao interface IngredientDao {
    @Query("SELECT * FROM ingredients ORDER BY favorite DESC, name, variant") fun observeAll(): Flow<List<Ingredient>>
    @Query("SELECT * FROM ingredients WHERE id = :id") suspend fun get(id: Long): Ingredient?
    @Query("SELECT COUNT(*) FROM ingredients") suspend fun count(): Int
    @Query("SELECT * FROM ingredients WHERE custom = 0") suspend fun builtIns(): List<Ingredient>
    @Query("DELETE FROM ingredients WHERE custom = 0") suspend fun deleteBuiltIns()
    @Insert suspend fun insert(value: Ingredient): Long
    @Insert suspend fun insertAll(values: List<Ingredient>)
    @Update suspend fun update(value: Ingredient)
}

@Dao interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY favorite DESC, lastUsedAt DESC, updatedAt DESC") fun observeAll(): Flow<List<Recipe>>
    @Transaction @Query("SELECT * FROM recipes ORDER BY favorite DESC, lastUsedAt DESC, updatedAt DESC")
    fun observeAllWithIngredients(): Flow<List<RecipeWithIngredients>>
    @Query("SELECT * FROM recipes WHERE id = :id") suspend fun get(id: Long): Recipe?
    @Transaction @Query("SELECT * FROM recipes WHERE id = :id") suspend fun getWithIngredients(id: Long): RecipeWithIngredients?
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :id ORDER BY id") suspend fun ingredients(id: Long): List<RecipeIngredient>
    @Insert suspend fun insert(value: Recipe): Long
    @Update suspend fun update(value: Recipe)
    @Query("DELETE FROM recipes WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :id") suspend fun clearIngredients(id: Long)
    @Insert suspend fun insertIngredients(values: List<RecipeIngredient>)
}

@Dao interface MealDao {
    @Transaction @Query("SELECT * FROM meals ORDER BY occurredAt DESC") fun observeAll(): Flow<List<MealWithItems>>
    @Transaction @Query("SELECT * FROM meals WHERE id = :id") suspend fun get(id: Long): MealWithItems?
    @Insert suspend fun insert(value: Meal): Long
    @Update suspend fun update(value: Meal)
    @Insert suspend fun insertItems(values: List<MealItem>)
    @Query("DELETE FROM meal_items WHERE mealId = :id") suspend fun clearItems(id: Long)
    @Query("DELETE FROM meals WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [Ingredient::class, Recipe::class, RecipeIngredient::class, Meal::class, MealItem::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ingredients(): IngredientDao
    abstract fun recipes(): RecipeDao
    abstract fun meals(): MealDao
    companion object {
        fun open(context: Context) = Room.databaseBuilder(context, AppDatabase::class.java, "raciones-t1d.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meals ADD COLUMN insulin TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE recipes SET portions = '1'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN manualCarbRations TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN manualCarbRations TEXT")
            }
        }
    }
}
