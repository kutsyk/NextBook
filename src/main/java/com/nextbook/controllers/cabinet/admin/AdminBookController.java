package com.nextbook.controllers.cabinet.admin;

import com.nextbook.domain.ResponseForAutoComplete;
import com.nextbook.domain.enums.Cover;
import com.nextbook.domain.enums.Status;
import com.nextbook.domain.enums.StatusChangedBy;
import com.nextbook.domain.filters.AuthorCriterion;
import com.nextbook.domain.filters.BookCriterion;
import com.nextbook.domain.filters.CommentsCriterion;
import com.nextbook.domain.filters.CommentsFilter;
import com.nextbook.domain.forms.book.BookRegisterForm;
import com.nextbook.domain.pojo.*;
import com.nextbook.domain.preview.AuthorPreview;
import com.nextbook.domain.upload.Constants;
import com.nextbook.services.*;
import com.nextbook.utils.SessionUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by Polomani on 27.07.2015.
 */
@Controller
@RequestMapping("/admin/books")
public class AdminBookController {

    @Inject
    private SessionUtils sessionUtils;
    @Inject
    private IBookProvider bookProvider;
    @Inject
    private ISubCategoryProvider subCategoryProvider;
    @Inject
    private IAuthorProvider authorProvider;
    @Inject
    private IPublisherProvider publisherProvider;
    @Inject
    private IKeywordProvider keywordProvider;
    @Inject
    private IBookStorageProvider bookStorageProvider;
    @Inject
    private ICommentsProvider commentsProvider;
    @Inject
    private IUserProvider userProvider;

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value="/all")
    public String allBooksPage(Model model) {
        model.addAttribute("books", bookProvider.getAllBooks());
        model.addAttribute("subCategories", subCategoryProvider.getAll());
        return "admin/books/manage-books";
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/edit-book", method = RequestMethod.GET)
    public String addBook(@RequestParam("bookId")int bookId,
                          Model model, Locale locale){
        Book book = bookProvider.getBookById(bookId);
        if(book == null)
            return "redirect:/cabinet/profile";
        model.addAttribute("subCategories", subCategoryProvider.getAll());
        model.addAttribute("book", book);
        model.addAttribute("authors", formAuthorsInLocale(book.getBookToAuthor(), locale.getLanguage()));
        model.addAttribute("numberOfPhotos", bookStorageProvider.getNumberOfPhotosInGallery(book.getId()));
        return "admin/books/add-book";
    }

    private List<AuthorPreview> formAuthorsInLocale(List<BookAuthor> authors, String language){
        List<AuthorPreview> result = new ArrayList<AuthorPreview>();
        if(authors != null) {
            for (BookAuthor bookAuthor : authors) {
                String name;
                Author author = bookAuthor.getAuthor();
                if (language.equals("uk")) {
                    name = author.getFirstNameUa() + ' ' + author.getLastNameUa();
                } else if (language.equals("ru")) {
                    name = author.getFirstNameRu() + ' ' + author.getLastNameRu();
                } else {
                    name = author.getFirstNameEn() + ' ' + author.getLastNameEn();
                }
                result.add(new AuthorPreview(author.getId(), name));
            }
        }
        return result;
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/edit-book", method = RequestMethod.POST, headers = "Accept=application/json")
    public @ResponseBody int editBook(@RequestBody BookRegisterForm bookRegisterForm){
        Book book = bookProvider.getBookById(bookRegisterForm.getBookId());
        if(book == null)
            return -1;
        String storageLink = bookStorageProvider.uploadBookToStorage(book.getId());
        if(storageLink == null)
            return -1;
        book.setLinkToStorage(storageLink);
        copyBookFromBookForm(book, bookRegisterForm);
        bookProvider.updateBook(book);
        return 1;
    }

    private void copyBookFromBookForm(Book book, BookRegisterForm bookRegisterForm){
        book.setIsbn(bookRegisterForm.getIsbn());
        book.setUaName(bookRegisterForm.getUaName());
        book.setEnName(bookRegisterForm.getEnName());
        book.setRuName(bookRegisterForm.getRuName());
        book.setEighteenPlus(bookRegisterForm.isEighteenPlus());
        book.setYearOfPublication(bookRegisterForm.getYearOfPublication());
        book.setLanguage(bookRegisterForm.getLanguage());
        book.setTypeOfBook(bookRegisterForm.getTypeOfBook());
        book.setNumberOfPages(bookRegisterForm.getNumberOfPages());
        book.setDescriptionUa(bookRegisterForm.getDescriptionUa());
        book.setDescriptionEn(bookRegisterForm.getDescriptionEn());
        book.setDescriptionRu(bookRegisterForm.getDescriptionRu());
        book.setSubCategory(subCategoryProvider.getById(bookRegisterForm.getSubCategoryId()));

        List<String> keywords = bookRegisterForm.getKeywords();
        for(String s : keywords){
            Keyword keyword = keywordProvider.getByName(s);
            if(keyword == null) {
                keyword = new Keyword();
                keyword.setKeyword(s);
                keyword = keywordProvider.update(keyword);
            }
            if(!book.getKeywords().contains(keyword)) {
                BookKeyword bookKeyword = new BookKeyword();
                bookKeyword.setBook(book);
                bookKeyword.setKeyword(keyword);
                book.addKeyword(keyword);
                bookProvider.updateBookToKeyword(bookKeyword);
            }
        }

        for(Integer id : bookRegisterForm.getAuthors()) {
            if(id == null)
                continue;
            Author author = authorProvider.getById(id);
            if(author != null) {
                BookAuthor bookAuthor = new BookAuthor();
                bookAuthor.setAuthor(author);
                bookAuthor.setBook(book);
                bookAuthor = bookProvider.updateBookToAuthor(bookAuthor);
                if(bookAuthor != null)
                    book.addAuthor(bookAuthor.getAuthor());
            }
        }
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping (value = "/filter", method = RequestMethod.POST, headers = "Accept=application/json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody List<Book> getBooksByCriterion(@RequestBody BookCriterion criterion,
                                                 Locale locale) {
        String language = locale.getLanguage();
        List<Book> books = bookProvider.getBooksByCriterion(criterion);
//        List<BookMainInfo> res = new ArrayList<BookMainInfo>();
//        if (books!=null)
//            for (Book book: books)
//                res.add(new BookMainInfo(book, language));
        return books;
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/authors-auto-complete/{keyword}", method = RequestMethod.POST)
    public @ResponseBody List<ResponseForAutoComplete> authorsAutoComplete(@PathVariable("keyword") String keyword,
                                                                           Locale locale){
        if(keyword.equals(""))
            return new ArrayList<ResponseForAutoComplete>();
        List<Author> authors = authorProvider.getAuthorsByCriterion(new AuthorCriterion(keyword));
        String language = locale.getLanguage();
        List<ResponseForAutoComplete> response = formAuthorsForAutoComplete(authors, language);
        return response;
    }

    private List<ResponseForAutoComplete> formAuthorsForAutoComplete(List<Author> authors, String language){
        List<ResponseForAutoComplete> result = new ArrayList<ResponseForAutoComplete>();
        if(authors != null) {
            for (Author author : authors) {
                String value;
                if (language.equals("uk")) {
                    value = author.getFirstNameUa() + ' ' + author.getLastNameUa();
                } else if (language.equals("ru")) {
                    value = author.getFirstNameRu() + ' ' + author.getLastNameRu();
                } else {
                    value = author.getFirstNameEn() + ' ' + author.getLastNameEn();
                }
                result.add(new ResponseForAutoComplete(author.getId(), value));
            }
        }
        return result;
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/keywords-auto-complete/{keyword}", method = RequestMethod.POST)
    public @ResponseBody List<ResponseForAutoComplete> keywordsAutoComplete(@PathVariable("keyword") String keyword,
                                                                            Locale locale){
        if(keyword.equals(""))
            return new ArrayList<ResponseForAutoComplete>();
        List<Keyword> keywords = keywordProvider.getListByKeyword(keyword);
        List<ResponseForAutoComplete> response = new ArrayList<ResponseForAutoComplete>();
        if(keywords != null) {
            for (Keyword k : keywords) {
                response.add(new ResponseForAutoComplete(k.getId(), k.getKeyword()));
            }
        }
        return response;
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/send-first-page", method = RequestMethod.POST)
    public @ResponseBody boolean firstPage(@RequestParam("first_page")MultipartFile file,
                                           @RequestParam("bookId") int bookId){
        return saveCover(bookId, file, Cover.FIRST_PAGE);
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/send-last-page", method = RequestMethod.POST)
    public @ResponseBody boolean lastPage(@RequestParam("last_page")MultipartFile file,
                                          @RequestParam("bookId") int bookId){
        return saveCover(bookId, file, Cover.LAST_PAGE);
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/send-book", method = RequestMethod.POST)
    public @ResponseBody boolean uploadBook(@RequestParam("book")MultipartFile file,
                                            @RequestParam("bookId") int bookId){
        return saveBook(bookId, file);
    }

    private boolean saveCover(int bookId, MultipartFile file, Cover cover){
        if(file == null)
            return false;
        Book book = bookProvider.getBookById(bookId);
        if(book == null)
            return false;
        boolean success = bookStorageProvider.uploadCoversToLocalStorage(bookId, file, cover);
        return success;
    }

    private boolean saveBook(int bookId, MultipartFile file){
        if(file == null)
            return false;
        Book book = bookProvider.getBookById(bookId);
        if(book == null)
            return false;
        boolean success = bookStorageProvider.uploadBookToLocalStorage(bookId, file);
        return success;
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/send-gallery-photo", method = RequestMethod.POST)
    public @ResponseBody int upload(MultipartHttpServletRequest request,
                                    @RequestParam(value = "bookId", required = true) Integer bookId) {
        if (bookId == null || bookId == 0)
            return -1;
        Book book = bookProvider.getBookById(bookId);
        if(book == null)
            return -1;
        Iterator<String> itr =  request.getFileNames();
        MultipartFile multipartFile = request.getFile(itr.next());
        bookStorageProvider.uploadGalleryPhoto(book.getId(), multipartFile);
        return bookStorageProvider.getNumberOfPhotosInGallery(bookId);
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/check-isbn/{isbn}", method = RequestMethod.POST)
    public @ResponseBody boolean isbnExist(@PathVariable("isbn") String isbn){
        if(isbn == null)
            return false;
        return bookProvider.isbnExist(isbn);
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/delete-gallery-image/{bookId}/{photoId}", method = RequestMethod.POST)
    public @ResponseBody int deleteGalleryImage(@PathVariable("bookId") int bookId,
                                                @PathVariable("photoId") int photoId){
        boolean success = bookStorageProvider.deleteGalleryPhoto(bookId, photoId);
        if(!success)
            return -1;
        return bookStorageProvider.getNumberOfPhotosInGallery(bookId);
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/view/{id}", method = RequestMethod.GET)
    public String viewBook(@PathVariable("id") int bookId,
                           Model model){

        Book book = bookProvider.getBookById(bookId);
        if(book == null)
            return "redirect:/admin/books/all";

        String url = book.getLinkToStorage();
        if(url == null || url.equals(""))
            return "404";
        model.addAttribute("urlToFile", url);
        model.addAttribute("pass", Constants.USER_PASSWORD);
        return "book/view";
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/delete-keyword/{bookId}/{keywordId}", method = RequestMethod.POST)
    public @ResponseBody boolean deleteKeyword(@PathVariable("bookId") int bookId,
                                               @PathVariable("keywordId") int keywordId){
        Book book = bookProvider.getBookById(bookId);
        if(book == null)
            return false;
        boolean success = bookProvider.deleteBookToKeyword(bookId, keywordId);
        return success;
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/delete-author/{bookId}/{authorId}", method = RequestMethod.POST)
    public @ResponseBody boolean deleteAuthor(@PathVariable("bookId") int bookId,
                                              @PathVariable("authorId") int authorId){
        Book book = bookProvider.getBookById(bookId);
        if(book == null)
            return false;
        boolean success = bookProvider.deleteBookToAuthor(bookId, authorId);
        return success;
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/allComments", method = RequestMethod.GET)
    public String allComments(@ModelAttribute("commentsCriterion") CommentsFilter filter,
                              Model model){
        CommentsCriterion criterion = copyFromCommentsFilter(filter);
        List<Comment> comments = commentsProvider.getCommentsByCriterion(criterion);
        model.addAttribute("comments", comments);

        copyFromCommentsCriterion(criterion, filter);

        model.addAttribute("commentsCriterion", filter);
        model.addAttribute("statuses", Status.values());
        model.addAttribute("changedByValues", StatusChangedBy.values());

        return "admin/books/all-comments";
    }


    public CommentsCriterion copyFromCommentsFilter(CommentsFilter filter){
        CommentsCriterion criterion = new CommentsCriterion(filter);


        criterion.setUser(userProvider.getById(filter.getUserId()));
        criterion.setBook(bookProvider.getBookById(filter.getBookId()));

        criterion.setFrom(filter.getPage() * COMMENTS_PER_PAGE);
        criterion.setMax(COMMENTS_PER_PAGE);

        return criterion;
    }


    public void copyFromCommentsCriterion(CommentsCriterion criterion, CommentsFilter filter){
        if(filter == null)
            filter = new CommentsFilter();
        filter.setStatus(criterion.getStatus());
        filter.setTimeTo(criterion.getTimeTo());
        filter.setTimeFrom(criterion.getTimeFrom());
        filter.setBookId(criterion.getBook() != null ? criterion.getBook().getId() : 0);
        filter.setUserId(criterion.getUser() != null ? criterion.getUser().getId() : 0);
        filter.setChangedBy(criterion.getChangedBy());
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/activateComment/{commentId}", method = RequestMethod.POST)
    public @ResponseBody boolean activateComment(@PathVariable("commentId") int commentId){
        Comment comment = commentsProvider.getById(commentId);
        if(comment == null)
            return false;
        comment = commentsProvider.adminActivateComment(comment);

        return comment != null;
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/deactivateComment/{commentId}", method = RequestMethod.POST)
    public @ResponseBody boolean deactivateComment(@PathVariable("commentId") int commentId){
        Comment comment = commentsProvider.getById(commentId);
        if(comment == null)
            return false;
        comment = commentsProvider.adminDeactivateComment(comment);

        return comment != null;
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/deleteComment/{commentId}", method = RequestMethod.POST)
    public @ResponseBody boolean deleteComment(@PathVariable("commentId") int commentId){
        Comment comment = commentsProvider.getById(commentId);
        if(comment == null)
            return false;

        boolean success = commentsProvider.removeComment(comment);

        return success;
    }

    @PreAuthorize("@Secure.isAdmin()")
    @RequestMapping(value = "/deactivateAllUserComments", method = RequestMethod.GET)
    public String deactivateAllUserComments(@RequestParam("userId") int userId){
        User user = userProvider.getById(userId);
        if(user != null) {
            CommentsCriterion criterion = new CommentsCriterion();
            criterion.setUser(user);
            List<Comment> userComments = commentsProvider.getCommentsByCriterion(criterion);
            for(Comment comment : userComments){
                commentsProvider.adminDeactivateComment(comment);
            }
        }
        return "redirect:/admin/books/allComments";
    }

    private static final int COMMENTS_PER_PAGE = 100;
}
