package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Categories
import com.controlfinanciero.models.dto.CategoryDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class CategoryRepository {

    private fun ResultRow.toCategoryDTO() = CategoryDTO(
        id = this[Categories.id],
        name = this[Categories.name],
        type = this[Categories.type],
        icon = this[Categories.icon],
        color = this[Categories.color]
    )

    suspend fun getAll(userId: Int): List<CategoryDTO> = dbQuery {
        Categories.selectAll().where { Categories.userId eq userId }
            .map { it.toCategoryDTO() }
    }

    suspend fun getByType(userId: Int, type: String): List<CategoryDTO> = dbQuery {
        Categories.selectAll()
            .where { (Categories.userId eq userId) and (Categories.type eq type) }
            .map { it.toCategoryDTO() }
    }

    suspend fun getById(userId: Int, id: Int): CategoryDTO? = dbQuery {
        Categories.selectAll()
            .where { (Categories.id eq id) and (Categories.userId eq userId) }
            .singleOrNull()?.toCategoryDTO()
    }

    /** Primera categoría del usuario (preferentemente de egreso). Útil como default para Mercado Pago. */
    suspend fun firstCategoryId(userId: Int): Int? = dbQuery {
        Categories.selectAll().where { Categories.userId eq userId }
            .orderBy(Categories.type to SortOrder.DESC, Categories.id to SortOrder.ASC) // "egreso" antes que "ingreso"
            .limit(1)
            .singleOrNull()?.get(Categories.id)
    }

    suspend fun create(userId: Int, category: CategoryDTO): CategoryDTO = dbQuery {
        val id = Categories.insert {
            it[Categories.userId] = userId
            it[name] = category.name
            it[type] = category.type
            it[icon] = category.icon
            it[color] = category.color
            it[createdAt] = LocalDateTime.now()
        } get Categories.id

        category.copy(id = id)
    }

    suspend fun update(userId: Int, id: Int, category: CategoryDTO): Boolean = dbQuery {
        Categories.update({ (Categories.id eq id) and (Categories.userId eq userId) }) {
            it[name] = category.name
            it[type] = category.type
            it[icon] = category.icon
            it[color] = category.color
        } > 0
    }

    suspend fun delete(userId: Int, id: Int): Boolean = dbQuery {
        Categories.deleteWhere { (Categories.id eq id) and (Categories.userId eq userId) } > 0
    }

    suspend fun seedDefaults(userId: Int) = dbQuery {
        val alreadyHas = Categories.selectAll().where { Categories.userId eq userId }.count() > 0L
        if (!alreadyHas) {
            val defaults = listOf(
                CategoryDTO(name = "Haberes", type = "ingreso", icon = "work", color = "#4CAF50"),
                CategoryDTO(name = "Trabajo extra", type = "ingreso", icon = "laptop", color = "#8BC34A"),
                CategoryDTO(name = "Inversiones", type = "ingreso", icon = "trending_up", color = "#00BCD4"),
                CategoryDTO(name = "Otros ingresos", type = "ingreso", icon = "add_circle", color = "#009688"),
                CategoryDTO(name = "Alimentación", type = "egreso", icon = "restaurant", color = "#FF5722"),
                CategoryDTO(name = "Transporte", type = "egreso", icon = "directions_car", color = "#FF9800"),
                CategoryDTO(name = "Servicios", type = "egreso", icon = "receipt", color = "#FFC107"),
                CategoryDTO(name = "Entretenimiento", type = "egreso", icon = "movie", color = "#E91E63"),
                CategoryDTO(name = "Salud", type = "egreso", icon = "local_hospital", color = "#F44336"),
                CategoryDTO(name = "Educación", type = "egreso", icon = "school", color = "#3F51B5"),
                CategoryDTO(name = "Hogar", type = "egreso", icon = "home", color = "#795548"),
                CategoryDTO(name = "Otros egresos", type = "egreso", icon = "remove_circle", color = "#9E9E9E"),
            )
            defaults.forEach { cat ->
                Categories.insert {
                    it[Categories.userId] = userId
                    it[name] = cat.name
                    it[type] = cat.type
                    it[icon] = cat.icon
                    it[color] = cat.color
                    it[createdAt] = LocalDateTime.now()
                }
            }
        }
    }
}
