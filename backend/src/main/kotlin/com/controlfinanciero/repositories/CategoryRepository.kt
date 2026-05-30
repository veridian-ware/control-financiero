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

    suspend fun getAll(): List<CategoryDTO> = dbQuery {
        Categories.selectAll().map { it.toCategoryDTO() }
    }

    suspend fun getByType(type: String): List<CategoryDTO> = dbQuery {
        Categories.selectAll().where { Categories.type eq type }
            .map { it.toCategoryDTO() }
    }

    suspend fun getById(id: Int): CategoryDTO? = dbQuery {
        Categories.selectAll().where { Categories.id eq id }
            .singleOrNull()?.toCategoryDTO()
    }

    suspend fun create(category: CategoryDTO): CategoryDTO = dbQuery {
        val id = Categories.insert {
            it[name] = category.name
            it[type] = category.type
            it[icon] = category.icon
            it[color] = category.color
            it[createdAt] = LocalDateTime.now()
        } get Categories.id

        category.copy(id = id)
    }

    suspend fun update(id: Int, category: CategoryDTO): Boolean = dbQuery {
        Categories.update({ Categories.id eq id }) {
            it[name] = category.name
            it[type] = category.type
            it[icon] = category.icon
            it[color] = category.color
        } > 0
    }

    suspend fun delete(id: Int): Boolean = dbQuery {
        Categories.deleteWhere { Categories.id eq id } > 0
    }

    suspend fun seedDefaults() = dbQuery {
        if (Categories.selectAll().count() == 0L) {
            val defaults = listOf(
                CategoryDTO(name = "Salario", type = "ingreso", icon = "work", color = "#4CAF50"),
                CategoryDTO(name = "Freelance", type = "ingreso", icon = "laptop", color = "#8BC34A"),
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
