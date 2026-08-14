package com.umg.sgq.web;

import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.*;

@RestControllerAdvice
public class ManejadorExcepcionesApi {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e){
        Map<String,String> errors=new LinkedHashMap<>(); e.getBindingResult().getFieldErrors().forEach(x->errors.put(x.getField(),x.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message","Revise los campos ingresados","errors",errors));
    }
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})
    ResponseEntity<?> business(RuntimeException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
    @ExceptionHandler({OptimisticLockException.class,OptimisticLockingFailureException.class})
    ResponseEntity<?> concurrency(Exception e){return ResponseEntity.status(409).body(Map.of("message","La operación fue rechazada porque la información cambió durante el proceso."));}
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> upload(MaxUploadSizeExceededException e){return ResponseEntity.badRequest().body(Map.of("message","El archivo supera el tamaño máximo permitido de 2 MB."));}
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> generic(Exception e){return ResponseEntity.status(500).body(Map.of("message","Error interno del sistema. Intente nuevamente."));}
}
