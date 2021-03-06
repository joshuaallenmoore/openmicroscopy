#!/usr/bin/env python
# 
# 
# 
# Copyright (c) 2008 University of Dundee. 
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
# Author: Aleksandra Tarkowska <A(dot)Tarkowska(at)dundee(dot)ac(dot)uk>, 2008.
# 
# Version: 1.0
#


import datetime
import traceback
import logging

from django.conf import settings
from django import template

register = template.Library()

logger = logging.getLogger('custom_tags')


@register.filter
def hash(value, key):
    return value[key]

@register.filter
def truncateafter(value, arg):
    """
    Truncates a string after a given number of chars  
    Argument: Number of chars to truncate after
    """
    try:
        length = int(arg)
    except ValueError: # invalid literal for int()
        return value # Fail silently.
    if not isinstance(value, basestring):
        value = str(value)
    if (len(value) > length):
        return value[:length] + "..."
    else:
        return value

@register.filter
def truncatebefor(value, arg):
    """
    Truncates a string after a given number of chars  
    Argument: Number of chars to truncate befor
    """
    try:
        length = int(arg)
    except ValueError: # invalid literal for int()
        return value # Fail silently.
    if not isinstance(value, basestring):
        value = str(value)
    if (len(value) > length):
        return "..."+value[len(value)-length:]
    else:
        return value

@register.filter
def warphtml(value, arg):
    """ Split words longer than arg, adding ' ' so that they wrap when displayed in web """
    try:
        length = int(arg)
    except ValueError: # invalid literal for int()
        return value # Fail silently.
    
    if not isinstance(value, basestring):
        value = str(value)  
    try:
        result = []
        for w in value.split(" "):
            l = len(w)
            if l < length:
                result.append(w)
            # only interested in splitting words longer than limit..
            elif l >= length:
                for i, s in enumerate(w.split("|")):  # ... try splitting bit more first
                    if i>0:
                        s = "|"+s
                    if len(s) < length:
                        result.append(s)
                    else:
                        for v in range(0,len(value),length):
                            result.append(s[v:v+length])
        return " ".join(result)
    except Exception, x:
        logger.error(traceback.format_exc())
        return value

@register.filter
def shortening(value, arg):
    try:
        length = int(arg)
    except ValueError: # invalid literal for int()
        return value # Fail silently.
    front = length/2-3
    end = length/2-3
    
    if not isinstance(value, basestring):
        value = str(value)  
    try: 
        l = len(value) 
        if l < length: 
            return value
        elif l >= length: 
            return value[:front]+"..."+value[l-end:]
    except Exception, x:
        logger.error(traceback.format_exc())
        return value

# makes settings available in template
@register.tag
def setting ( parser, token ): 
    try:
        tag_name, option = token.split_contents()
    except ValueError:
        raise template.TemplateSyntaxError, "%r tag requires a single argument" % token.contents[0]
    return SettingNode( option )

class SettingNode ( template.Node ): 
    def __init__ ( self, option ): 
        self.option = option

    def render ( self, context ): 
        # if FAILURE then FAIL silently
        try:
            return str(settings.__getattr__(self.option))
        except:
            return ""

class PluralNode(template.Node):
    def __init__(self, quantity, single, plural):
        self.quantity = template.Variable(quantity)
        self.single = template.Variable(single)
        self.plural = template.Variable(plural)

    def render(self, context):
        if self.quantity.resolve(context) == 1:
            return u'%s' % self.single.resolve(context)
        else:
            return u'%s' % self.plural.resolve(context)

@register.tag(name="plural")
def do_plural(parser, token):
    """
    Usage: {% plural quantity name_singular name_plural %}

    This simple version only works with template variable since we will use blocktrans for strings.
    """
    
    try:
        # split_contents() knows not to split quoted strings.
        tag_name, quantity, single, plural = token.split_contents()
    except ValueError:
        raise template.TemplateSyntaxError, "%r tag requires exactly three arguments" % token.contents.split()[0]

    return PluralNode(quantity, single, plural)
