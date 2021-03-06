package org.uengine.five.service;

import org.metaworks.dwr.MetaworksRemoteService;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.uengine.five.DefinitionServiceApplication;
import org.uengine.five.entity.Definition;
import org.uengine.five.entity.DefinitionVersion;
import org.uengine.five.repository.DefinitionRepository;
import org.uengine.five.repository.DefinitionVersionRepository;
import org.uengine.modeling.resource.Serializer;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.Serializable;

/**
 * Created by uengine on 2018. 1. 23..
 */
@Component
@RestController
public class DefinitionServiceImpl implements DefinitionXMLService {

    @RequestMapping(value = DefinitionService.DEFINITION_RAW + "/{defPath}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public Object getRawDefinition(@PathVariable("defPath") String defId) throws Exception{

        if(defId.endsWith(".json")){
            defId = defId.substring(0, defId.length() - 4);
        }

        String verId = null;

        if(defId.indexOf("@") > 0){

            String[] defIdAndVersion = defId.split("@");
            defId = defIdAndVersion[0];
            verId = defIdAndVersion[1];
        }


        if(verId!=null){

            DefinitionVersionRepository definitionVersionRepository = MetaworksRemoteService.getComponent(DefinitionVersionRepository.class);
            DefinitionVersion definitionVersion;

            if("last".equals(verId)){
                DefinitionRepository definitionRepository = MetaworksRemoteService.getComponent(DefinitionRepository.class);
                Definition definition = definitionRepository.findById(defId).orElse(null);

                definitionVersion = definition.getVersions().get(definition.getVersions().size()-1);
            }else{
                definitionVersion = definitionVersionRepository.findById(new Long(verId)).orElse(null);
            }

            ByteArrayInputStream bai = new ByteArrayInputStream(definitionVersion.getRawFile());

            Serializable definition = (Serializable) Serializer.deserialize(bai);
            DefinitionWrapper definitionWrapper = new DefinitionWrapper();
            definitionWrapper.setDefinition(definition);

            return DefinitionServiceApplication.createTypedJsonObjectMapper().writeValueAsString(definitionWrapper);

        }else {
            DefinitionRepository definitionRepository = MetaworksRemoteService.getComponent(DefinitionRepository.class);
            Definition definition = definitionRepository.findById(defId).orElse(null);

            return definition.getDefinitionJson();
        }
    }

    @Override
    @RequestMapping(value = DefinitionService.DEFINITION + "/xml/{defPath}", method = RequestMethod.GET, produces = "application/xml;charset=UTF-8")
    public String getXMLDefinition(@PathVariable("defPath") String defId, @RequestParam("production") boolean production) throws Exception{
        if(defId.endsWith(".xml")){
            defId = defId.substring(0, defId.length() - 4);
        }

        String verId = null;

        if(defId.indexOf("@") > 0){

            String[] defIdAndVersion = defId.split("@");
            defId = defIdAndVersion[0];
            verId = defIdAndVersion[1];
        }


        if(verId!=null){
            DefinitionVersionRepository definitionVersionRepository = MetaworksRemoteService.getComponent(DefinitionVersionRepository.class);
            DefinitionVersion definitionVersion = definitionVersionRepository.findById(new Long(verId)).orElse(null);

            return new String(definitionVersion.getRawFile());

        }else{

            // Definition.afterLoadOne() will store the definition xml at the production version.

            DefinitionRepository definitionRepository = MetaworksRemoteService.getComponent(DefinitionRepository.class);
            Definition definition = definitionRepository.findById(defId).orElse(null);

            return definition.getDefinitionXml();

        }
    }

    /**
     * http POST localhost:8080/definition/raw/keyboard < keyboard.json
     * http POST localhost:8080/instance?defPath=keyboard
     * @param defId
     * @param json
     * @throws Exception
     */
    @RequestMapping(value = DefinitionService.DEFINITION_RAW + "/{defId}", method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional
    public void putRawDefinition(@PathVariable("defId") String defId, @RequestBody String json) throws Exception {
        String verId = null;

        if(defId.indexOf("@") > 0){

            String[] defIdAndVersion = defId.split("@");
            defId = defIdAndVersion[0];
            verId = defIdAndVersion[1];
        }

        Definition definition;

        DefinitionRepository definitionRepository = MetaworksRemoteService.getComponent(DefinitionRepository.class);
        definition = definitionRepository.findById(defId).orElse(null);

        if(definition==null){
            definition = new Definition();
            definition.setDefId(defId);
        }

        definition.setDefinitionJson(json);

        definitionRepository.save(definition);
    }

    @RequestMapping(value = "/version/{defVerId}/production", method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional
    public ResourceSupport makeProduction(@PathVariable("defVerId") String defVerId) throws Exception {
        DefinitionVersionRepository  definitionVersionRepository = MetaworksRemoteService.getComponent(DefinitionVersionRepository.class);

        DefinitionVersion definitionVersion = definitionVersionRepository.findById(new Long(defVerId)).orElse(null);

        definitionVersion.getDefinition().setProdVerId(definitionVersion.getDefVerId());
        definitionVersionRepository.save(definitionVersion);

        DefinitionResource definitionResource = new DefinitionResource();
        definitionResource.add(new Link("/version/" + defVerId, "self"));
        return definitionResource;
    }


//    @RequestMapping(value = DEFINITION + "/_versions/{defId}", method = {RequestMethod.GET})
//    public List getVersions(@PathVariable("defId") String defId) throws Exception {
//
//        DefinitionRepository definitionRepository = MetaworksRemoteService.getComponent(DefinitionRepository.class);
//        definitionRepository.findOne(defId);
//
//
//    }

}