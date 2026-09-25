// Presentation helpers only: no API requests, storage, role checks, or form handling.
export function badge(node, state) {
  node.classList.add('status-badge');
  node.dataset.state = String(state).replaceAll(' ', '_');
  return node;
}

export function summaryCard(item, label, value) {
  const name = document.createElement('span');
  name.className = 'summary-label';
  name.textContent = label;
  const count = document.createElement('strong');
  count.className = 'summary-value';
  count.textContent = value;
  item.replaceChildren(name, count);
}

// Use only existing local service photography; unknown categories get a text-only card.
const photos = new Set(['after_school_homework_helper', 'air_conditioner_electrical_wiring', 'aquarium_and_exotic_care', 'at_home_pet_sitting', 'babysitting', 'backyard_and_garden_cleaning', 'barbecue_grill_special', 'basic_pet_grooming_and_bathing', 'biometric_access_control_setup', 'birthday_party_finger_food', 'catering', 'cctv_camera_system_installation', 'ceiling_fan_and_light_installation', 'cleaning', 'commercial_store_security', 'complete_house_re_wiring', 'corporate_lunch_catering', 'dog_walking_and_exercise', 'electrician', 'emergency_on_call_care', 'full_home_deep_cleaning', 'full_time_daytime_nanny', 'infant_care_specialist', 'ips_and_generator_maintenance', 'kitchen_deep_degreasing', 'night_shift_residential_guard', 'office_workspace_cleaning', 'overnight_pet_boarding', 'personal_bodyguard_protection', 'pet_caring', 'pet_vet_appointment_escort', 'post_construction_cleaning', 'private_event_security_guard', 'private_home_chef_experience', 'puppy_training_companion', 'religious_festival_feast', 'security', 'short_circuit_troubleshooting', 'smart_alarm_infrastructure', 'smart_home_device_setup', 'sofa_upholstery_shampooing', 'special_needs_child_care', 'wedding_buffet_catering', 'weekend_night_babysitter']);
export function serviceImage(slug) {
  const name = slug === 'pet-care' ? 'pet_caring' : String(slug || '').replaceAll('-', '_');
  if (!photos.has(name)) return null;
  const image = document.createElement('img');
  image.src = `/images/${name}.jpg`;
  image.alt = ''; // The adjacent category/service title supplies the accessible name.
  image.loading = 'lazy';
  image.width = 400;
  image.height = 240;
  return image;
}
