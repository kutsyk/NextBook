package com.nextbook.services;

import com.nextbook.dao.impl.CategoryDAO;
import com.nextbook.domain.pojo.Category;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by Polomani on 26.09.2015.
 */
public interface ICategoryProvider {

    Category getById(int id);

    public List<Category> getAll();

    Category getByLink(String link);
}
