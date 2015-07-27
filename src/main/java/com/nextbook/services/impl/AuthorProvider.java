package com.nextbook.services.impl;

import com.nextbook.dao.IAuthorDao;
import com.nextbook.dao.impl.AuthorDao;
import com.nextbook.domain.pojo.Author;
import com.nextbook.services.IAuthorProvider;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: borsch
 * Date: 7/24/2015
 * Time: 3:49 PM
 */
public class AuthorProvider implements IAuthorProvider{

    private IAuthorDao authorDao = new AuthorDao();

    @Override
    public Author updateAuthor(Author author) {
        if(author == null)
            return null;
        return authorDao.updateAuthor(author);
    }

    @Override
    public boolean deleteAuthor(int authorId) {
        return authorDao.deleteAuthor(authorId);
    }

    @Override
    public Author getById(int authorId) {
        return authorDao.getById(authorId);
    }

    @Override
    public List<Author> getAll() {
        return authorDao.getFromMax(0, 0);
    }

    @Override
    public List<Author> getFromMax(int from, int max) {
        return authorDao.getFromMax(from, max);
    }
}