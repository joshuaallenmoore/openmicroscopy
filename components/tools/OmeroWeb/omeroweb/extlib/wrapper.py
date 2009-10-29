#!/usr/bin/env python
# 
# Wrapper
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
#         Carlos Neves <carlos(at)glencoesoftware(dot)com>, 2008
# 
# Version: 1.0
#

import cStringIO
import traceback
import logging

logger = logging.getLogger('gateway')

try:
    import Image,ImageDraw
except:
    logger.error("You need to install the Python Imaging Library. Get it at http://www.pythonware.com/products/pil/")
    logger.error(traceback.format_exc())
from StringIO import StringIO

#import threading
import time
from datetime import datetime
from types import IntType, ListType, TupleType, UnicodeType, StringType

from django.utils.translation import ugettext as _
from django.conf import settings
from django.core.mail import send_mail
from django.core.mail import EmailMultiAlternatives

import Ice
import Glacier2
import omero
import omero_api_IScript_ice
from omero.rtypes import *

from omero.gateway import timeit

from omero_model_FileAnnotationI import FileAnnotationI
from omero_model_TagAnnotationI import TagAnnotationI
from omero_model_DatasetI import DatasetI
from omero_model_ProjectI import ProjectI
from omero_model_ImageI import ImageI
from omero_model_DetectorI import DetectorI
from omero_model_FilterI import FilterI
from omero_model_ObjectiveI import ObjectiveI
from omero_model_InstrumentI import InstrumentI

from omero_sys_ParametersI import ParametersI

class OmeroWebObjectWrapper (object):

    child_counter = None
    annotation_counter = None
    
    def __prepare__ (self, **kwargs):
        try:
            self.child_counter = kwargs['child_counter']
        except:
            pass
        try:
            self.annotation_counter = kwargs['annotation_counter']
        except:
            pass
    
    def countChildren2 (self):
        #return len(list(self.listChildren()))
        logger.debug(str(self)+'.countChildren2')
        if self.child_counter is not None:
            return self.child_counter
        else:
            return self.countChildren()
    
    def listAnnotations (self):
        #container = self._conn.getContainerService()
        meta = self._conn.getMetadataService()
        self.annotations = meta.loadAnnotations(self._obj.__class__.__name__, [self._oid], None, None, None).get(self._oid, [])
        for ann in self.annotations:
            yield AnnotationWrapper(self._conn, ann)
    
    def countAnnotations (self):
        if self.annotation_counter is not None:
            return self.annotation_counter
        else:
            container = self._conn.getContainerService()
            m = container.getCollectionCount(self._obj.__class__.__name__, type(self._obj).ANNOTATIONLINKS, [self._oid], None)
            if m[self._oid] > 0:
                self.annotation_counter = m[self._oid]
                return self.annotation_counter
            else:
                return None
    
    def isOwned(self):
        return (self._obj.details.owner.id.val == self._conn.getEventContext().userId)
    
    def accessControll(self):
        if self._obj.details.permissions.isUserRead() and self._obj.details.permissions.isUserWrite():
            return '0'
        elif self._obj.details.permissions.isGroupRead() and self._obj.details.permissions.isGroupWrite():
            return '1'
        elif self._obj.details.permissions.isWorldRead() and self._obj.details.permissions.isWorldWrite():
            return '2'
        else:
            return '-1'

    def splitedName(self):
        try:
            name = self._obj.name.val
            l = len(name)
            if l < 45:
                return name
            elif l >= 45:
                splited = []
                for v in range(0,len(name),45):
                    splited.append(name[v:v+45]+"\n")
                return "".join(splited)
        except:
            logger.info(traceback.format_exc())
            return self._obj.name.val
    
    def fullNameWrapped(self):
        try:
            name = self._obj.name.val
            l = len(name)
            if l <= 60:
                return name
            elif l > 60:
                splited = []
                for v in range(0,len(name),60):
                    splited.append(name[v:v+60]+"\n")
                return "".join(splited)
        except:
            logger.info(traceback.format_exc())
            return self._obj.name.val
    
    def shortName(self):
        try:
            name = self._obj.name.val
            l = len(name)
            if l < 55:
                return name
            return "..." + name[l - 55:]
        except:
            logger.info(traceback.format_exc())
            return self._obj.name.val
    
    def tinyName(self):
        try:
            name = self._obj.name.val
            l = len(name)
            if l <= 20:
                return name
            elif l > 20 and l <= 40:
                splited = []
                for v in range(0,len(name),20):
                    splited.append(name[v:v+20]+"\n")
                return "".join(splited)
            elif l > 40:
                nname = "..." + name[l - 36:]
                splited = list()
                for v in range(0,len(nname),20):
                    splited.append(nname[v:v+20]+"\n")
                return "".join(splited)
        except:
            logger.info(traceback.format_exc())
            return self._obj.name.val
    
    def breadcrumbName(self):
        name = None
        try:
            name = self._obj.name.val
            l = len(name)
            if l <= 20:
                return name
            elif l > 20 and l < 30:
                splited = []
                for v in range(0,len(name),20):
                    splited.append(name[v:v+20])
                return "".join(splited)
            elif l >= 30:
                nname = "..." + name[l - 30:]
                return nname
        except:
            name = self._obj.textValue.val
            l = len(name)
            if l <= 100:
                return name
            elif l > 100:
                return name[:45] + "..." + name[l - 45:]
        return None
    
    def shortDescription(self):
        try:
            desc = self._obj.description
            if desc == None or desc.val == "":
                return None
            l = len(desc.val)
            if l < 550:
                return desc.val
            return desc.val[:550] + "..."
        except:
            logger.info(traceback.format_exc())
            return self._obj.description.val
    
    def tinyDescription(self):
        try:
            desc = self._obj.description
            if desc == None or desc.val == "":
                return None
            l = len(desc.val)
            if l <= 28:
                return desc.val
            return desc.val[:28] + "..."
        except:
            logger.info(traceback.format_exc())
            return self._obj.description.val

class ExperimenterWrapper (OmeroWebObjectWrapper, omero.gateway.ExperimenterWrapper):
#    LINK_NAME = "copyGroupExperimenterMap"
#    OMERO_CLASS = 'Experimetner'
#    PARENT_WRAPPER_CLASS = 'ExperimenterGroup'
#    
    def shortInstitution(self):
        try:
            inst = self._obj.institution
            if inst == None or inst.val == "":
                return "-"
            l = len(inst.val)
            if l < 30:
                return inst.val
            return inst.val[:30] + "..."
        except:
            logger.error(traceback.format_exc())
            return None

    def getFullName(self):
        try:
            if self.middleName is not None and self.middleName != '':
                name = "%s %s. %s" % (self.firstName, self.middleName[:1], self.lastName)
            else:
                name = "%s %s" % (self.firstName, self.lastName)
            
            l = len(name)
            if l < 40:
                return name
            return name[:40] + "..."
        except:
            logger.error(traceback.format_exc())
            return _("Unknown name")
    
    def getInitialName(self):
        try:
            if self.firstName is not None and self.lastName is not None:
                name = "%s. %s" % (self.firstName[:1], self.lastName)
            else:
                name = self.omeName
            return name
        except:
            logger.error(traceback.format_exc())
            return _("Unknown name")

omero.gateway.ExperimenterWrapper = ExperimenterWrapper

class ExperimenterGroupWrapper (OmeroWebObjectWrapper, omero.gateway.ExperimenterGroupWrapper):
    pass

omero.gateway.ExperimenterGroupWrapper = ExperimenterGroupWrapper

#    LINK_NAME = "copyGroupExperimenterMap"
#    OMERO_CLASS = 'ExperimenterGroup'
#    LINK_CLASS = 'GroupExperimenterMap'
#    CHILD_WRAPPER_CLASS = 'Experimenter'
#    
#class GroupWrapper (BlitzObjectWrapper):
#    LINK_CLASS = None
#    CHILD_WRAPPER_CLASS = None

class ScriptWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    pass

class ProjectWrapper (OmeroWebObjectWrapper, omero.gateway.ProjectWrapper):
    pass

omero.gateway.ProjectWrapper = ProjectWrapper

class DatasetWrapper (OmeroWebObjectWrapper, omero.gateway.DatasetWrapper):
    pass

omero.gateway.DatasetWrapper = DatasetWrapper

class AnnotationLinkWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):

    def getAnnotation(self):
        return omero.gateway.AnnotationWrapper(self, self.child)

class AnnotationWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    
    def isOriginalMetadat(self):
        if isinstance(self._obj, FileAnnotationI):
            try:
                if self._obj.ns.val == omero.constants.namespaces.NSCOMPANIONFILE and self._obj.file.name.val.startswith("original_metadata"):
                    return True
            except:
                logger.info(traceback.format_exc())
        return False
     
    def getFileSize(self):
        if isinstance(self._obj, FileAnnotationI):
            return self._obj.file.size.val

    def getFileName(self):
        if isinstance(self._obj, FileAnnotationI):
            try:
                name = self._obj.file.name.val
                l = len(name)
                if l < 65:
                    return name
                return name[:30] + "..." + name[l - 30:] 
            except:
                logger.info(traceback.format_exc())
                return self._obj.file.name.val
    
    def shortTag(self):
        if isinstance(self._obj, TagAnnotationI):
            try:
                name = self._obj.textValue.val
                l = len(name)
                if l < 17:
                    return name
                return name[:7] + "..." + name[l - 7:] 
            except:
                logger.info(traceback.format_exc())
                return self._obj.textValue.val

class ImageImagingEnvironmentWrapper (omero.gateway.BlitzObjectWrapper):
    pass

class ImageObjectiveSettingsWrapper (omero.gateway.BlitzObjectWrapper):
    pass

class ImageObjectiveWrapper (omero.gateway.BlitzObjectWrapper):
    pass

class ImageImmersionWrapper (omero.gateway.BlitzObjectWrapper):
    pass

class ImageCorrectionWrapper (omero.gateway.BlitzObjectWrapper):
    pass

class ImageInstrumentWrapper (omero.gateway.BlitzObjectWrapper):
    pass

class ImageFilterWrapper (omero.gateway.BlitzObjectWrapper):
    
    def getTransmittanceRange(self):
        if self._obj.transmittanceRange is None:
            return None
        else:
            return FilterTransmittanceRangeWrapper(self._conn, self._obj.transmittanceRange)

    def getFilterType(self):
        if self._obj.type is None:
            return None
        else:
            return TypeWrapper(self._conn, self._obj.type)

class FilterTransmittanceRangeWrapper (omero.gateway.BlitzObjectWrapper):
    pass

class ImageDetectorWrapper (omero.gateway.BlitzObjectWrapper):
    
    def getDetectorType(self):
        if self._obj.type is None:
            return None
        else:
            return TypeWrapper(self._conn, self._obj.type)
   
class TypeWrapper (omero.gateway.BlitzObjectWrapper):
    pass

class ImageStageLabelWrapper (omero.gateway.BlitzObjectWrapper):
    pass

class ImageWrapper (OmeroWebObjectWrapper, omero.gateway.ImageWrapper):
    
    def getThumbnail (self, size=(120,120)):
        rv = super(omero.gateway.ImageWrapper, self).getThumbnail(size=size)
        if rv is None:
            try:
                rv = self.defaultThumbnail(size)
            except Exception, e:
                logger.info(traceback.format_exc())
                raise e
        return rv
    
    def defaultThumbnail(self, size=(120,120)):
        img = Image.open(settings.DEFAULT_IMG)
        img.thumbnail(size, Image.ANTIALIAS)
        draw = ImageDraw.Draw(img)
        f = cStringIO.StringIO()
        img.save(f, "PNG")
        f.seek(0)
        return f.read()
    
    # metadata getters
    # from metadata service
    def getMicroscopInstruments(self):
        meta_serv = self._conn.getMetadataService()
        if self._obj.instrument is None:
            yield None
        else:
            for inst in meta_serv.loadInstrument(self._obj.instrument.id.val):
                if isinstance(inst, InstrumentI):
                    yield ImageInstrumentWrapper(self._conn, inst)
    
    def getMicroscopDetectors(self):
        meta_serv = self._conn.getMetadataService()
        if self._obj.instrument is None:
            yield None
        else:
            for inst in meta_serv.loadInstrument(self._obj.instrument.id.val):
                if isinstance(inst, DetectorI):
                    yield ImageDetectorWrapper(self._conn, inst)
    
    def getMicroscopFilters(self):
        meta_serv = self._conn.getMetadataService()
        if self._obj.instrument is None:
            yield None
        else:
            for inst in meta_serv.loadInstrument(self._obj.instrument.id.val):
                if isinstance(inst, FilterI):
                    yield ImageFilterWrapper(self._conn, inst)
    
    # from model
    def getImagingEnvironment(self):
        if self._obj.imagingEnvironment is None:
            return None
        else:
            return ImageImagingEnvironmentWrapper(self._conn, self._obj.imagingEnvironment)
    
    def getObjectiveSettings(self):
        if self._obj.objectiveSettings is None:
            return None
        else:
            return ImageObjectiveSettingsWrapper(self._conn, self._obj.objectiveSettings)
    
    def getMedium(self):
        if self._obj.objectiveSettings.medium is None:
            return None
        else:
            return EnumerationWrapper(self._conn, self._obj.objectiveSettings.medium)

    def getObjective(self):
        if self._obj.objectiveSettings.objective is None:
            return None
        else:
            return ImageObjectiveWrapper(self._conn, self._obj.objectiveSettings.objective)
    
    def getImmersion(self):
        if self._obj.objectiveSettings.objective.immersion is None:
            return None
        else:
            return ImageImmersionWrapper(self._conn, self._obj.objectiveSettings.objective.immersion)
    
    def getCorrection(self):
        if self._obj.objectiveSettings.objective.correction is None:
            return None
        else:
            return ImageCorrectionWrapper(self._conn, self._obj.objectiveSettings.objective.correction)

    def getStageLabel (self):
        if self._obj.stageLabel is None:
            return None
        else:
            return ImageStageLabelWrapper(self._conn, self._obj.stageLabel)

omero.gateway.ImageWrapper = ImageWrapper
    
class ChannelWrapper (omero.gateway.ChannelWrapper):
            
    def getLogicalChannel(self):
        meta_serv = self._conn.getMetadataService()
        if self._obj is None:
            return None
        elif self._obj.logicalChannel is None:
            return None
        else:
            lc = meta_serv.loadChannelAcquisitionData([long(self._obj.logicalChannel.id.val)])
            if lc is not None and len(lc) > 0:
                return LogicalChannelWrapper(self._conn, lc[0])
            return None
            
omero.gateway.ChannelWrapper = ChannelWrapper

class LogicalChannelWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    
    def getIllumination(self):
        if self._obj.illumination is None:
            return None
        else:
            return EnumerationWrapper(self._conn, self._obj.illumination)
    
    def getContrastMethod(self):
        if self._obj.contrastMethod is None:
            return None
        else:
            return EnumerationWrapper(self._conn, self._obj.contrastMethod)
    
    def getMode(self):
        if self._obj.mode is None:
            return None
        else:
            return EnumerationWrapper(self._conn, self._obj.mode)
    
    def getEmissionFilter(self):
        if self._obj.secondaryEmissionFilter is None:
            return None
        else:
            return ImageFilterWrapper(self._conn, self._obj.secondaryEmissionFilter)
    
    def getDichroic(self):
        if self._obj.filterSet is None:
            return None
        elif self._obj.filterSet.dichroic is None:
            return None
        else:
            return DichroicWrapper(self._conn, self._obj.filterSet.dichroic)
    
    def getDetectorSettings(self):
        if self._obj.detectorSettings is None:
            return None
        elif self._obj.detectorSettings.detector is None:
            return None
        else:
            return ImageDetectorWrapper(self._conn, self._obj.detectorSettings.detector)
    
    def getLightSource(self):
        if self._obj.lightSourceSettings is None:
            return None
        elif self._obj.lightSourceSettings.lightSource is None:
            return None
        else:
            return LightSourceWrapper(self._conn, self._obj.lightSourceSettings.lightSource)

class LightSourceWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    
    def getLightSourceType(self):
        if self._obj.type is None:
            return None
        else:
            return TypeWrapper(self._conn, self._obj.type)
    
    def getLaserMedium(self):
        if self._obj.laserMedium is None:
            return None
        else:
            return EnumerationWrapper(self._conn, self._obj.laserMedium)
    
    def getPulse(self):
        if self._obj.pulse is None:
            return None
        else:
            return EnumerationWrapper(self._conn, self._obj.pulse)
    
class DichroicWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    pass

class DatasetImageLinkWrapper (omero.gateway.BlitzObjectWrapper):
    pass

class ProjectDatasetLinkWrapper (omero.gateway.BlitzObjectWrapper):
    pass
    
class ScreenWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
            
    def listChildren (self):
        """ return a generator yielding child objects """
        try:
            childnodes = [ x.child for x in getattr(self._obj, self.LINK_NAME)()]

            #child_ids = [child.id.val for child in childnodes]
            #child_counter = None
            #if len(child_ids) > 0:
            #    child_counter = self._conn.getCollectionCount(self.CHILD, \
            #        (PlateWrapper.LINK_NAME[4].lower() + \
            #        PlateWrapper.LINK_NAME[5:]), child_ids)
            #    child_annotation_counter = self._conn.getCollectionCount(self.CHILD, "annotationLinks", child_ids)
            for child in childnodes:
            #    kwargs = dict()
            #    if child_counter:
            #        kwargs['child_counter'] = child_counter.get(child.id.val)
            #    if child_annotation_counter:
            #        kwargs['annotation_counter'] = child_annotation_counter.get(child.id.val)
                yield PlateWrapper(self._conn, child)
        except:
            raise NotImplementedError

ScreenWrapper = ScreenWrapper

class PlateWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    pass

PlateWrapper = PlateWrapper

class WellWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    
    def __bstrap__ (self):
        self.OMERO_CLASS = 'Well'
        self.LINK_CLASS = "WellSample"
        self.CHILD_WRAPPER_CLASS = "ImageWrapper"
        self.PARENT_WRAPPER_CLASS = 'PlateWrapper'
    
    def __prepare__ (self, **kwargs):
        try:
            self.index = int(kwargs['index'])
        except:
            self.index = 0
    
    def isWellSample (self):
        """ return boolean if object exist """
        if getattr(self, 'isWellSamplesLoaded')():
            childnodes = getattr(self, 'copyWellSamples')()
            logger.debug('listChildren for %s %d: already loaded, %d samples' % (self.OMERO_CLASS, self.getId(), len(childnodes)))
            if len(childnodes) > 0:
                return True
        return False
    
    def countWellSample (self):
        """ return boolean if object exist """
        if getattr(self, 'isWellSamplesLoaded')():
            childnodes = getattr(self, 'copyWellSamples')()
            logger.debug('countChildren for %s %d: already loaded, %d samples' % (self.OMERO_CLASS, self.getId(), len(childnodes)))
            size = len(childnodes)
            if size > 0:
                return size
        return 0
    
    def selectedWellSample (self):
        """ return a wrapped child object """
        if getattr(self, 'isWellSamplesLoaded')():
            childnodes = getattr(self, 'copyWellSamples')()
            logger.debug('listSelectedChildren for %s %d: already loaded, %d samples' % (self.OMERO_CLASS, self.getId(), len(childnodes)))
            if len(childnodes) > 0:
                return WellSampleWrapper(self._conn, childnodes[self.index])
        return None
    
    def loadWellSamples (self):
        """ return a generator yielding child objects """
        if getattr(self, 'isWellSamplesLoaded')():
            childnodes = getattr(self, 'copyWellSamples')()
            logger.debug('listChildren for %s %d: already loaded, %d samples' % (self.OMERO_CLASS, self.getId(), len(childnodes)))
            for ch in childnodes:
                yield WellSampleWrapper(self._conn, ch)
    
    def plate(self):
        return PlateWrapper(self._conn, self._obj.plate)
    
WellWrapper = WellWrapper

class WellSampleWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    
    def image(self):
        return ImageWrapper(self._conn, self._obj.image)

WellSampleWrapper = WellSampleWrapper

class ShareWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    
    def shortMessage(self):
        try:
            msg = self.getMessage().val
            l = len(msg)
            if l < 50:
                return msg
            return msg[:50] + "..."
        except:
            logger.info(traceback.format_exc())
            return None
    
    def tinyMessage(self):
        try:
            msg = self.getMessage().val
            l = len(msg)
            if l < 20:
                return msg
            elif l >= 20:
                return "%s..." % (msg[:20])
        except:
            logger.info(traceback.format_exc())
            return None
    
    def getShareType(self):
        if self.itemCount == 0:
            return "Discuss"
        else:
            return "Share"
    
    def getMembersCount(self):
        return "None"
        
    def getStartDate(self):
        return datetime.fromtimestamp(self.getStarted().val/1000)
        
    def getExpirationDate(self):
        try:
            return datetime.fromtimestamp((self.getStarted().val+self.getTimeToLive().val)/1000)
        except ValueError:
            return None
        except:
            return None
    
    def isExpired(self):
        try:
            if (self.getStarted().val+self.getTimeToLive().val)/1000 <= time.time():
                return True
            else:
                return False
        except:
            return True
        
    # Owner methods had to be updated because share.details.owner does not exist. Share.owner.
    def isOwned(self):
        if self.owner.id.val == self._conn.getEventContext().userId:
            return True
        else:
            return False
    
    def getOwnerAsExperimetner(self):
        return omero.gateway.ExperimenterWrapper(self, self.owner)
    
    def getShareOwnerFullName(self):
        try:
            lastName = self.owner.lastName and self.owner.lastName.val or ''
            firstName = self.owner.firstName and self.owner.firstName.val or ''
            middleName = self.owner.middleName and self.owner.middleName.val or ''
            
            if middleName is not None and middleName != '':
                name = "%s %s. %s" % (firstName, middleName, lastName)
            else:
                name = "%s %s" % (firstName, lastName)
            return name
        except:
            logger.info(traceback.format_exc())
            return None
    
class ShareContentWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    pass

class ShareCommentWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    pass
    
class SessionAnnotationLinkWrapper (OmeroWebObjectWrapper, omero.gateway.BlitzObjectWrapper):
    def getComment(self):
        return ShareCommentWrapper(self._conn, self.child)
    
    def getShare(self):
        return ShareWrapper(self._conn, self.parent)
    
class EventLogWrapper (omero.gateway.BlitzObjectWrapper):
    LINK_CLASS = "EventLog"

class EnumerationWrapper (omero.gateway.BlitzObjectWrapper):
    
    def getType(self):
        return self._obj.__class__