package com.rent.luxury.controller;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rent.luxury.model.Alquiler;
import com.rent.luxury.model.AlquilerForm;
import com.rent.luxury.model.Contacto;
import com.rent.luxury.model.ContactoForm;
import com.rent.luxury.model.Role;
import com.rent.luxury.model.Usuarios;
import com.rent.luxury.model.UsuariosForm;
import com.rent.luxury.model.Vehiculos;
import com.rent.luxury.model.VehiculosForm;
import com.rent.luxury.repository.AlquilerRepositorio;
import com.rent.luxury.repository.ContactoRepositorio;
import com.rent.luxury.repository.RoleRepository;
import com.rent.luxury.repository.UsuariosRepositorio;
import com.rent.luxury.repository.VehiculosRepositorio;
import com.rent.luxury.services.AlquilerService;
import com.rent.luxury.services.UsuarioService;
import com.rent.luxury.services.VehiculoService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class ControladorPrincipal {
	@Autowired
	private UsuariosRepositorio usuariosRepositorio;
	@Autowired
	private RoleRepository rolesRepositorio;
	@Autowired
	private VehiculosRepositorio vehiculosRepositorio;
	@Autowired
	private AlquilerRepositorio alquilerRepositorio;
	@Autowired
	private ContactoRepositorio contactoRepositorio;
	@Autowired
	private AlquilerService alquilerServicio;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private VehiculoService vehiculoService;

//////////////////////////////////////////////////////////////////////////////////////////////////////////    
	@GetMapping(path = "/inicio")
	public String iniPg(Model modelo) {
	    List<Vehiculos> vehiculos = (List<Vehiculos>) vehiculosRepositorio.findByAlquiladoFalse();
	    Date fechaActual = new Date();
	    for (Alquiler alquiler : alquilerRepositorio.findAll()) {
	        if (alquiler.getFechaRecogida().before(fechaActual) && (alquiler.getFechaEntrega() == null || alquiler.getFechaEntrega().after(fechaActual))) {
	            vehiculos.remove(alquiler.getVehiculo());
	        }
	    }
	    
	    // Ordenar los vehículos por precio de forma ascendente
	    Collections.sort(vehiculos, new Comparator<Vehiculos>() {
	        @Override
	        public int compare(Vehiculos v1, Vehiculos v2) {
	            return Double.compare(v1.getPreciopordia(), v2.getPreciopordia());
	        }
	    });
	    
	    // Obtener los primeros 6 vehículos con el precio más bajo
	    List<Vehiculos> vehiculosDestacados = vehiculos.subList(0, Math.min(6, vehiculos.size()));
	    
	    modelo.addAttribute("lista", vehiculosDestacados);
	    return "index";
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@GetMapping("/catalogovehiculos")
	public String getCatalogo(Model modelo) {
	    List<Vehiculos> vehiculos = (List<Vehiculos>) vehiculosRepositorio.findByAlquiladoFalse();
	    Date fechaActual = new Date();
	    for (Alquiler alquiler : alquilerRepositorio.findAll()) {
	        if (alquiler.getFechaRecogida().before(fechaActual) && (alquiler.getFechaEntrega() == null || alquiler.getFechaEntrega().after(fechaActual))) {
	        	vehiculos.remove(alquiler.getVehiculo());
	        }
	    }
	    modelo.addAttribute("lista", vehiculos);
		return "catalogo";
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@GetMapping("/iniciarsesion")
	public String getInicioSesion(UsuariosForm usuForm) {
		return "/iniciarsesion";
	}
	@PostMapping("/iniciarsesion")
	public String checksesionini(@Valid UsuariosForm usuForm, BindingResult bindingResult, HttpSession session,
	Model modelo) {
	String email = usuForm.getEmail();
	String passwd = usuForm.getPassword();
	Optional<Usuarios> usuarioEncontrado = usuariosRepositorio.findByEmail(email);
	if (usuarioEncontrado.isPresent()) {
	// Almacenar el rol del usuario en la variable de sesión
	session.setAttribute("rol", usuarioEncontrado.get().getRole().getNombrerol());
	// Almacenar el objeto Usuario en la sesión
	session.setAttribute("usuario", usuarioEncontrado.get());
	
	if (usuarioEncontrado.get().getRole().getNombrerol().equals("ADMIN")
	&& usuarioEncontrado.get().getPassword().equals(passwd)) {
	return "redirect:/inicio";
	}  else if (usuarioEncontrado.get().getRole().getNombrerol().equals("CLIENTE")
	&& usuarioEncontrado.get().getPassword().equals(passwd)) {
	return "redirect:/inicio";
	}
	}
	modelo.addAttribute("mensaje",
	"El correo " + usuForm.getEmail() + " no existe, o la contraseña es incorrecta.");
	return "iniciarsesion";
	}
	
	@GetMapping("/cerrarsesion")
	public String cerrarSesion(HttpSession session) {
	session.invalidate();
	return "redirect:/inicio";
	}
	
	@PostMapping("/darseDeBaja")
	public String darseDeBaja(HttpSession session, Model modelo) {
	    // Obtener el usuario en sesión
	    Usuarios usuario = (Usuarios) session.getAttribute("usuario");
	    
	    // Lógica para dar de baja al usuario
	    try {
	        // Eliminar al usuario de la base de datos
	        usuariosRepositorio.delete(usuario);
	        
	        // Eliminar la sesión del usuario
	        session.invalidate();
	        
	    } catch (Exception e) {
	        modelo.addAttribute("error", "Error al darse de baja");
	    }
	    
	    return "redirect:/inicio";
	}
	
	@PostMapping("/cambiarContraseña")
	public String cambiarContraseña(@RequestParam("currentPassword") String currentPassword,
	                                @RequestParam("newPassword") String newPassword,
	                                @RequestParam("renewPassword") String renewPassword,
	                                HttpSession session, Model model) {
	    // Obtener el usuario actualmente autenticado
	    Usuarios usuario = (Usuarios) session.getAttribute("usuario");
	    
	    // Verificar que la contraseña actual coincida
	    if (!usuario.getPassword().equals(currentPassword)) {
	        model.addAttribute("error", "La contraseña actual no es válida");
	        return "profile";
	    }
	    
	    // Verificar que la nueva contraseña y la confirmación coincidan
	    if (!newPassword.equals(renewPassword)) {
	        model.addAttribute("error", "La nueva contraseña y la confirmación no coinciden");
	        return "profile";
	    }
	    
	    // Verificar la fortaleza de la nueva contraseña (ejemplo: longitud mínima de 8 caracteres)
	    if (newPassword.length() < 8) {
	        model.addAttribute("error", "La nueva contraseña debe tener al menos 8 caracteres");
	        return "profile";
	    }
	    
	    // Verificar otras reglas de validación de contraseñas según tus requisitos
	    
	    // Actualizar la contraseña del usuario
	    usuario.setPassword(newPassword);
	    usuariosRepositorio.save(usuario);
	    
	    model.addAttribute("mensaje", "¡Contraseña cambiada exitosamente!");
	    return "profile";
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@GetMapping("/modificarusuarioperfil")
	public String getmodificardatosperfil(Model modelo, HttpSession session) {
	    Usuarios usuario = (Usuarios) session.getAttribute("usuario");
	    
	    modelo.addAttribute("usuario", usuario);

	    return "profile";
	}

	@PostMapping("/modificarusuarioperfil")
	public String modificarUsuarioPerfil(@ModelAttribute("usuario") Usuarios usuario, BindingResult result) {
	    if (result.hasErrors()) {
	        return "profile";
	    }
	    usuarioService.actualizarUsuario(usuario);
	    return "redirect:/profile";
	}


	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@GetMapping("/about")
	public String getInfo(UsuariosForm usuForm) {
		return "/about";
	}
	
	@GetMapping("/services")
	public String getSericios(UsuariosForm usuForm) {
		return "/services";
	}
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@GetMapping("/profile")
	public String getperfil(Model modelo, HttpSession session) {
	    Usuarios usuario = (Usuarios) session.getAttribute("usuario");

	    // Obtener las reservas del usuario
	    List<Alquiler> reservas = alquilerServicio.obtenerReservasPorUsuario(usuario);

	    // Agregar las reservas al modelo
	    modelo.addAttribute("reservas", reservas);

	    // Agregar el usuario al modelo
	    modelo.addAttribute("usuario", usuario);

	    // Devolver la vista "profile.html"
	    return "/profile";
	}



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@GetMapping("/register")
	public String getRegistro(UsuariosForm usuForm) {
		return "/register";
	}

	@GetMapping("/contactar")
	public String getInsertarvehiculos(ContactoForm contactoForm, Model modelo) {
		return "/contact";
	}

	@PostMapping(path = "/contactar")
	public String checkvehiculosInfo(@Valid ContactoForm contactoForm, BindingResult bindingResult, Model modelo) {
		if (bindingResult.hasErrors()) {
			modelo.addAttribute("mensaje", "");
			return "/contact";
		}
		Contacto contacto = new Contacto(contactoForm.getNombre(), contactoForm.getEmail(), contactoForm.getMensaje());
		contactoRepositorio.save(contacto);
		modelo.addAttribute("mensaje", "Su mensaje ha sido enviado satisfactoriamente. Por favor, espere a que nos pongamos en contacto con usted.");
        // Limpiar los campos
        ContactoForm nuevoContactoForm = new ContactoForm();
        modelo.addAttribute("contactoForm", nuevoContactoForm);
		return "/contact";
	}
	
}
