package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.CategoryDTO
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.CategoryRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.categoryRoutes() {
    val repository = CategoryRepository()

    route("/api/categories") {
        get {
            val type = call.queryParameters["type"]
            val categories = if (type != null) repository.getByType(call.userId(), type)
            else repository.getAll(call.userId())
            call.respond(ApiResponse(true, data = categories))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val category = repository.getById(call.userId(), id)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Categoría no encontrada"))
            call.respond(ApiResponse(true, data = category))
        }

        post {
            val dto = call.receive<CategoryDTO>()
            val created = repository.create(call.userId(), dto)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = created))
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val dto = call.receive<CategoryDTO>()
            val updated = repository.update(call.userId(), id, dto)
            if (updated) call.respond(ApiResponse(true, data = "Categoría actualizada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Categoría no encontrada"))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val deleted = repository.delete(call.userId(), id)
            if (deleted) call.respond(ApiResponse(true, data = "Categoría eliminada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Categoría no encontrada"))
        }

        post("/seed") {
            repository.seedDefaults(call.userId())
            call.respond(ApiResponse(true, data = "Categorías por defecto creadas"))
        }
    }
}
