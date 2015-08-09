package com.nextbook.controllers.cabinet.user;

import com.nextbook.domain.forms.publishers.SimplePublisherForm;
import com.nextbook.domain.pojo.Book;
import com.nextbook.domain.pojo.Publisher;
import com.nextbook.domain.pojo.User;
import com.nextbook.services.IBookProvider;
import com.nextbook.services.IPublisherProvider;
import com.nextbook.services.IUserProvider;
import com.nextbook.utils.SessionUtils;
import org.omg.CORBA.Request;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Polomani on 24.07.2015.
 */
@Controller
@RequestMapping("/publisher")
public class PublishersController {

    @Inject
    private IPublisherProvider publisherProvider;
    @Inject
    private SessionUtils sessionUtils;
    @Inject
    private IBookProvider bookProvider;

    @RequestMapping(value="/add")
    @PreAuthorize("hasRole('ROLE_PUBLISHER')")
    public String addPublisher(Model model,
                               @RequestParam (required = false, defaultValue = "false") boolean first) {
        if (publisherProvider.getPublisherByUser(sessionUtils.getCurrentUser())!=null)
            return "redirect:/cabinet/profile";
        model.addAttribute("first", first);
        model.addAttribute("edit", false);
        return "/publisher/edit-publisher";
    }

    @RequestMapping(value="/update/{id}")
    @PreAuthorize("hasRole('ROLE_PUBLISHER')")
    public String updatePublisher(Model model,
                                  @PathVariable int id,
                                  @RequestParam (defaultValue = "false", required = false) boolean first) {
        Publisher publisher = publisherProvider.getPublisherById(id);
        Publisher upublisher = publisherProvider.getPublisherByUser(sessionUtils.getCurrentUser());
        if (publisher==null || upublisher==null || publisher.getId() != upublisher.getId())
            return "redirect:/cabinet/profile";
        model.addAttribute("publisher", publisher);
        model.addAttribute("first", first);
        model.addAttribute("edit", true);
        return "/publisher/edit-publisher";
    }

    @RequestMapping(value="/update", method = RequestMethod.POST, headers = "Accept=application/json")
    @PreAuthorize("hasRole('ROLE_PUBLISHER')")
    public @ResponseBody
    Publisher updatePublisher(@RequestBody SimplePublisherForm form) {
        Publisher result = null;
        Publisher publisher = null;
        User user = sessionUtils.getCurrentUser();
        Publisher upublisher = publisherProvider.getPublisherByUser(user);
        if (form.getId()!=0)
            publisher = publisherProvider.getPublisherById(form.getId());
        if (publisher==null) {
            if (upublisher!=null)
                return null;
            publisher = new Publisher();
            publisher.addUser(sessionUtils.getCurrentUser());
        } else {
            if (upublisher != null && publisher.getId() != upublisher.getId())
                return null;
        }
        publisher.setNameEn(form.getNameEn());
        publisher.setNameRu(form.getNameRu());
        publisher.setNameUa(form.getNameUa());
        publisher.setDescription(form.getDescription());
        result = publisherProvider.updatePublisher(publisher);
        return result;
    }

    @RequestMapping(value="/view", method = RequestMethod.GET)
    public String publisherPreview(@RequestParam("publisherId") int id,
                                   Model model) {
        User user = sessionUtils.getCurrentUser();
        if(user == null)
            return "redirect:/";
        Publisher publisher = publisherProvider.getPublisherById(id);
        if(publisher == null)
            return "redirect:/";
        if(!publisher.getUsers().contains(user))
            return "redirect:/";
        List<Book> books = bookProvider.getAllPublisherBooks(publisher.getId());
        List<User> users = publisher.getUsers();
        model.addAttribute("books", books);
        model.addAttribute("users", users);
        return "publisher/view-publisher";
    }

    @RequestMapping(value="/all")
    public @ResponseBody
    List<Publisher> getAllPublishers(@RequestParam (defaultValue = "0") int from, @RequestParam (defaultValue = "0")  int max) {
        return publisherProvider.getAllPublishers(from, max);
    }

}
