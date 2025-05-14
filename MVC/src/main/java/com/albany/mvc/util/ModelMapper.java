package com.albany.mvc.util;

import com.albany.mvc.dto.CompletedServiceDTO;
import com.albany.mvc.dto.LaborChargeDTO;
import com.albany.mvc.dto.MaterialItemDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility component for mapping between DTOs and Map objects
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ModelMapper {

    private final ObjectMapper objectMapper;

    /**
     * Convert a map to a CompletedServiceDTO
     */
    public CompletedServiceDTO mapToCompletedServiceDTO(Map<String, Object> data) {
        try {
            if (data == null) {
                return null;
            }

            // Use Jackson to convert the map to the DTO
            return objectMapper.convertValue(data, CompletedServiceDTO.class);
        } catch (Exception e) {
            log.error("Error mapping to CompletedServiceDTO: {}", e.getMessage(), e);

            // If Jackson conversion fails, try manual mapping
            return mapToCompletedServiceDTOManually(data);
        }
    }

    /**
     * Manually map a map to a CompletedServiceDTO
     */
    private CompletedServiceDTO mapToCompletedServiceDTOManually(Map<String, Object> data) {
        CompletedServiceDTO dto = new CompletedServiceDTO();

        try {
            // Basic info
            dto.setServiceId(getIntegerValue(data, "serviceId"));
            dto.setVehicleName(getStringValue(data, "vehicleName"));
            dto.setRegistrationNumber(getStringValue(data, "registrationNumber"));
            dto.setCustomerName(getStringValue(data, "customerName"));
            dto.setCustomerEmail(getStringValue(data, "customerEmail"));
            dto.setMembershipStatus(getStringValue(data, "membershipStatus"));
            dto.setServiceType(getStringValue(data, "serviceType"));
            dto.setAdditionalDescription(getStringValue(data, "additionalDescription"));
            dto.setStatus(getStringValue(data, "status"));
            dto.setCategory(getStringValue(data, "category"));
            dto.setVehicleBrand(getStringValue(data, "vehicleBrand"));
            dto.setVehicleModel(getStringValue(data, "vehicleModel"));

            // Dates
            dto.setRequestDate(getLocalDateValue(data, "requestDate"));
            dto.setCompletedDate(getLocalDateValue(data, "completedDate"));

            // Service advisor
            dto.setServiceAdvisorName(getStringValue(data, "serviceAdvisorName"));
            dto.setServiceAdvisorId(getIntegerValue(data, "serviceAdvisorId"));

            // Financial details
            dto.setMaterialsTotal(getBigDecimalValue(data, "materialsTotal"));
            dto.setLaborTotal(getBigDecimalValue(data, "laborTotal"));
            dto.setDiscount(getBigDecimalValue(data, "discount"));
            dto.setSubtotal(getBigDecimalValue(data, "subtotal"));
            dto.setTax(getBigDecimalValue(data, "tax"));
            dto.setTotalCost(getBigDecimalValue(data, "totalCost"));

            // Materials and labor
            dto.setMaterials(getMaterialsList(data));
            dto.setLaborCharges(getLaborChargesList(data));

            // Invoice and payment status - updated method names to match renamed fields
            dto.setHasBill(getBooleanValue(data, "hasBill"));
            dto.setPaid(getBooleanValue(data, "isPaid"));        // Changed from setIsPaid
            dto.setHasInvoice(getBooleanValue(data, "hasInvoice"));
            dto.setDelivered(getBooleanValue(data, "isDelivered")); // Changed from setIsDelivered

            // Invoice details
            dto.setInvoiceId(getIntegerValue(data, "invoiceId"));
            dto.setInvoiceDate(getLocalDateValue(data, "invoiceDate"));

            // Notes
            dto.setNotes(getStringValue(data, "notes"));
        } catch (Exception e) {
            log.error("Error during manual mapping to CompletedServiceDTO: {}", e.getMessage(), e);
        }

        return dto;
    }

    /**
     * Convert a CompletedServiceDTO to a map
     */
    public Map<String, Object> mapToMap(CompletedServiceDTO dto) {
        try {
            if (dto == null) {
                return Collections.emptyMap();
            }

            // Use Jackson to convert the DTO to a map
            return objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Error mapping CompletedServiceDTO to map: {}", e.getMessage(), e);

            // If Jackson conversion fails, try manual mapping
            return mapToMapManually(dto);
        }
    }

    /**
     * Manually map a CompletedServiceDTO to a map
     */
    private Map<String, Object> mapToMapManually(CompletedServiceDTO dto) {
        Map<String, Object> map = new HashMap<>();

        try {
            // Basic info
            if (dto.getServiceId() != null) map.put("serviceId", dto.getServiceId());
            if (dto.getVehicleName() != null) map.put("vehicleName", dto.getVehicleName());
            if (dto.getRegistrationNumber() != null) map.put("registrationNumber", dto.getRegistrationNumber());
            if (dto.getCustomerName() != null) map.put("customerName", dto.getCustomerName());
            if (dto.getCustomerEmail() != null) map.put("customerEmail", dto.getCustomerEmail());
            if (dto.getMembershipStatus() != null) map.put("membershipStatus", dto.getMembershipStatus());
            if (dto.getServiceType() != null) map.put("serviceType", dto.getServiceType());
            if (dto.getAdditionalDescription() != null) map.put("additionalDescription", dto.getAdditionalDescription());
            if (dto.getStatus() != null) map.put("status", dto.getStatus());
            if (dto.getCategory() != null) map.put("category", dto.getCategory());
            if (dto.getVehicleBrand() != null) map.put("vehicleBrand", dto.getVehicleBrand());
            if (dto.getVehicleModel() != null) map.put("vehicleModel", dto.getVehicleModel());

            // Dates
            if (dto.getRequestDate() != null) map.put("requestDate", dto.getRequestDate().toString());
            if (dto.getCompletedDate() != null) map.put("completedDate", dto.getCompletedDate().toString());

            // Service advisor
            if (dto.getServiceAdvisorName() != null) map.put("serviceAdvisorName", dto.getServiceAdvisorName());
            if (dto.getServiceAdvisorId() != null) map.put("serviceAdvisorId", dto.getServiceAdvisorId());

            // Financial details
            if (dto.getMaterialsTotal() != null) map.put("materialsTotal", dto.getMaterialsTotal());
            if (dto.getLaborTotal() != null) map.put("laborTotal", dto.getLaborTotal());
            if (dto.getDiscount() != null) map.put("discount", dto.getDiscount());
            if (dto.getSubtotal() != null) map.put("subtotal", dto.getSubtotal());
            if (dto.getTax() != null) map.put("tax", dto.getTax());
            if (dto.getTotalCost() != null) map.put("totalCost", dto.getTotalCost());

            // Materials and labor
            if (dto.getMaterials() != null && !dto.getMaterials().isEmpty()) {
                map.put("materials", dto.getMaterials().stream()
                        .map(this::mapMaterialItemToMap)
                        .collect(Collectors.toList()));
            }

            if (dto.getLaborCharges() != null && !dto.getLaborCharges().isEmpty()) {
                map.put("laborCharges", dto.getLaborCharges().stream()
                        .map(this::mapLaborChargeToMap)
                        .collect(Collectors.toList()));
            }

            // Invoice and payment status - use the same keys for backward compatibility
            map.put("hasBill", dto.isHasBill());
            map.put("isPaid", dto.isPaid());          // Still use isPaid as the key
            map.put("hasInvoice", dto.isHasInvoice());
            map.put("isDelivered", dto.isDelivered()); // Still use isDelivered as the key

            // Invoice details
            if (dto.getInvoiceId() != null) map.put("invoiceId", dto.getInvoiceId());
            if (dto.getInvoiceDate() != null) map.put("invoiceDate", dto.getInvoiceDate().toString());

            // Notes
            if (dto.getNotes() != null) map.put("notes", dto.getNotes());
        } catch (Exception e) {
            log.error("Error during manual mapping of CompletedServiceDTO to map: {}", e.getMessage(), e);
        }

        return map;
    }

    // Rest of the class methods remain unchanged

    /**
     * Map a MaterialItemDTO to a map
     */
    private Map<String, Object> mapMaterialItemToMap(MaterialItemDTO dto) {
        Map<String, Object> map = new HashMap<>();

        if (dto.getItemId() != null) map.put("itemId", dto.getItemId());
        if (dto.getName() != null) map.put("name", dto.getName());
        if (dto.getCategory() != null) map.put("category", dto.getCategory());
        if (dto.getQuantity() != null) map.put("quantity", dto.getQuantity());
        if (dto.getUnitPrice() != null) map.put("unitPrice", dto.getUnitPrice());
        if (dto.getTotal() != null) map.put("total", dto.getTotal());
        if (dto.getDescription() != null) map.put("description", dto.getDescription());

        return map;
    }

    /**
     * Map a LaborChargeDTO to a map
     */
    private Map<String, Object> mapLaborChargeToMap(LaborChargeDTO dto) {
        Map<String, Object> map = new HashMap<>();

        if (dto.getChargeId() != null) map.put("chargeId", dto.getChargeId());
        if (dto.getDescription() != null) map.put("description", dto.getDescription());
        if (dto.getHours() != null) map.put("hours", dto.getHours());
        if (dto.getRatePerHour() != null) map.put("ratePerHour", dto.getRatePerHour());
        if (dto.getTotal() != null) map.put("total", dto.getTotal());

        return map;
    }

    /**
     * Extract materials list from data map
     */
    @SuppressWarnings("unchecked")
    private List<MaterialItemDTO> getMaterialsList(Map<String, Object> data) {
        try {
            if (data.containsKey("materials") && data.get("materials") instanceof List) {
                List<Object> materialsList = (List<Object>) data.get("materials");

                return materialsList.stream()
                        .map(item -> {
                            if (item instanceof Map) {
                                Map<String, Object> materialMap = (Map<String, Object>) item;
                                MaterialItemDTO material = new MaterialItemDTO();
                                material.setItemId(getIntegerValue(materialMap, "itemId"));
                                material.setName(getStringValue(materialMap, "name"));
                                material.setCategory(getStringValue(materialMap, "category"));
                                material.setQuantity(getBigDecimalValue(materialMap, "quantity"));
                                material.setUnitPrice(getBigDecimalValue(materialMap, "unitPrice"));
                                material.setTotal(getBigDecimalValue(materialMap, "total"));
                                material.setDescription(getStringValue(materialMap, "description"));
                                return material;
                            } else if (item instanceof MaterialItemDTO) {
                                return (MaterialItemDTO) item;
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error extracting materials list: {}", e.getMessage(), e);
        }

        return new ArrayList<>();
    }

    /**
     * Extract labor charges list from data map
     */
    @SuppressWarnings("unchecked")
    private List<LaborChargeDTO> getLaborChargesList(Map<String, Object> data) {
        try {
            if (data.containsKey("laborCharges") && data.get("laborCharges") instanceof List) {
                List<Object> laborList = (List<Object>) data.get("laborCharges");

                return laborList.stream()
                        .map(item -> {
                            if (item instanceof Map) {
                                Map<String, Object> laborMap = (Map<String, Object>) item;
                                LaborChargeDTO labor = new LaborChargeDTO();
                                labor.setChargeId(getIntegerValue(laborMap, "chargeId"));
                                labor.setDescription(getStringValue(laborMap, "description"));
                                labor.setHours(getBigDecimalValue(laborMap, "hours"));
                                labor.setRatePerHour(getBigDecimalValue(laborMap, "ratePerHour"));
                                labor.setTotal(getBigDecimalValue(laborMap, "total"));
                                return labor;
                            } else if (item instanceof LaborChargeDTO) {
                                return (LaborChargeDTO) item;
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error extracting labor charges list: {}", e.getMessage(), e);
        }

        return new ArrayList<>();
    }

    /**
     * Helper method to get string value from a map
     */
    private String getStringValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return map.get(key).toString();
        }
        return null;
    }

    /**
     * Helper method to get integer value from a map
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof Integer) {
                return (Integer) map.get(key);
            } else if (map.get(key) instanceof Number) {
                return ((Number) map.get(key)).intValue();
            } else {
                try {
                    return Integer.parseInt(map.get(key).toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to get BigDecimal value from a map
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof BigDecimal) {
                return (BigDecimal) map.get(key);
            } else if (map.get(key) instanceof Number) {
                return BigDecimal.valueOf(((Number) map.get(key)).doubleValue());
            } else {
                try {
                    return new BigDecimal(map.get(key).toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to get LocalDate value from a map
     */
    private LocalDate getLocalDateValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof LocalDate) {
                return (LocalDate) map.get(key);
            } else {
                String dateStr = map.get(key).toString();

                // Try different date formats
                try {
                    return LocalDate.parse(dateStr);
                } catch (DateTimeParseException e1) {
                    try {
                        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    } catch (DateTimeParseException e2) {
                        try {
                            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                        } catch (DateTimeParseException e3) {
                            try {
                                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            } catch (DateTimeParseException e4) {
                                try {
                                    return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                                } catch (DateTimeParseException e5) {
                                    return null;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Helper method to get boolean value from a map
     */
    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof Boolean) {
                return (Boolean) map.get(key);
            } else {
                return Boolean.parseBoolean(map.get(key).toString());
            }
        }
        return false;
    }
}