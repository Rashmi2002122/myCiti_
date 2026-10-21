-- V11__backfill_listing_business_profiles.sql
-- Seed official organizer client accounts and ensure every listing links to a verified business profile

-- 1. Create seed organizer users with role = 'CLIENT' and status = 'ACTIVE'
INSERT INTO users (google_sub, email, name, business_name, role, status, profile_complete, client_type, created_at, last_login_at)
VALUES
('seed_user_bagbazar', 'bagbazar@organizer.myciti.in', 'Bagbazar Sarbojanin Samiti', 'Bagbazar Sarbojanin Durgotsav Samiti', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_collegesquare', 'collegesquare@organizer.myciti.in', 'College Square Committee', 'College Square Sarbojanin Committee', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_sreebhumi', 'sreebhumi@organizer.myciti.in', 'Sreebhumi Sporting Club', 'Sreebhumi Sporting Club', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_maddox', 'maddox@organizer.myciti.in', 'Maddox Square Puja Samiti', 'Maddox Square Puja Samiti', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_suruchi', 'suruchi@organizer.myciti.in', 'Suruchi Sangha Cultural Club', 'Suruchi Sangha Cultural Club', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_ekdalia', 'ekdalia@organizer.myciti.in', 'Ekdalia Evergreen Club', 'Ekdalia Evergreen Club', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_deshapriya', 'deshapriya@organizer.myciti.in', 'Deshapriya Park Samiti', 'Deshapriya Park Durgotsav Samiti', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_fdblock', 'fdblock@organizer.myciti.in', 'FD Block Residents Association', 'FD Block Residents Association', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_craftsguild', 'craftsguild@organizer.myciti.in', 'Bengal Crafts Guild', 'Bengal Crafts & Heritage Guild', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_weavers', 'weavers@organizer.myciti.in', 'Bengal Weavers Collective', 'Bengal Weavers & Artisans Collective', 'CLIENT', 'ACTIVE', true, 'STORE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_newmarket', 'newmarket@organizer.myciti.in', 'New Market Merchants', 'New Market Merchants Association', 'CLIENT', 'ACTIVE', true, 'STORE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_kalakriti', 'kalakriti@organizer.myciti.in', 'Kalakriti Boutique', 'Kalakriti Designer Boutique', 'CLIENT', 'ACTIVE', true, 'STORE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_rasogolla', 'rasogolla@organizer.myciti.in', 'Rasogolla & Co.', 'Rasogolla & Co. Bistro', 'CLIENT', 'ACTIVE', true, 'STORE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_kumartuli', 'kumartuli@organizer.myciti.in', 'Kumartuli Artisans Association', 'Kumartuli Artisans Association', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('seed_user_official', 'official@myciti.in', 'myCiti Community Organizers', 'myCiti Community Organizers', 'CLIENT', 'ACTIVE', true, 'COMPANY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- 2. Insert verified client profiles for each organizer
INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'bagbazar-sarbojanin',
    'Bagbazar Sarbojanin Durgotsav Samiti',
    'COMPANY',
    'Organizers of one of the oldest and most prestigious traditional Durga Pujas in North Kolkata since 1919.',
    'https://images.unsplash.com/photo-1596178065887-1198b6148b2b?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1601614212959-53696884617d?auto=format&fit=crop&w=1200&q=80',
    '7/1, Bagbazar St, Baghbazar, Kolkata, West Bengal 700003',
    'Near Bagbazar Launch Ghat',
    22.6025,
    88.3683,
    '+91-9830112233',
    true,
    '08:00:00',
    '02:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'bagbazar@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'college-square-sarbojanin',
    'College Square Sarbojanin Committee',
    'COMPANY',
    'Celebrated Durga Puja committee famed for mesmerizing water reflection illumination around College Square central lake.',
    'https://images.unsplash.com/photo-1567157577867-05ccb1388e66?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1567157577867-05ccb1388e66?auto=format&fit=crop&w=1200&q=80',
    '53, College St, College Square, Bowbazar, Kolkata, West Bengal 700073',
    'Opposite Calcutta University',
    22.5744,
    88.3639,
    '+91-9830223344',
    true,
    '10:00:00',
    '04:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'collegesquare@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'sreebhumi-sporting-club',
    'Sreebhumi Sporting Club',
    'COMPANY',
    'Renowned sporting club and cultural organizer famous for world-class architectural palace replicas and dazzling lighting.',
    'https://images.unsplash.com/photo-1545232979-8bf68ee9b1af?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=1200&q=80',
    'Lake Town Rd, Sreebhumi, Lake Town, Kolkata, West Bengal 700048',
    'Near VIP Road Footover Bridge',
    22.5991,
    88.4005,
    '+91-9830334455',
    true,
    '09:00:00',
    '05:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'sreebhumi@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'maddox-square-puja',
    'Maddox Square Puja Samiti',
    'COMPANY',
    'Iconic South Kolkata puja and adda hub bringing together youth, music, cultural adda, and festive food stalls.',
    'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=1200&q=80',
    'Pankaj Mullick Sarani, Ballygunge, Kolkata, West Bengal 700019',
    'Ritchie Road Corner',
    22.5292,
    88.3601,
    '+91-9830445566',
    true,
    '08:00:00',
    '03:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'maddox@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'suruchi-sangha',
    'Suruchi Sangha Cultural Club',
    'COMPANY',
    'Pioneering cultural club celebrating eco-friendly art installations and indigenous handicraft heritage from across India.',
    'https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80',
    'State Bank Park, New Alipore, Kolkata, West Bengal 700053',
    'Near Nalini Ranjan Avenue',
    22.5085,
    88.3287,
    '+91-9830556677',
    true,
    '09:00:00',
    '03:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'suruchi@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'ekdalia-evergreen-club',
    'Ekdalia Evergreen Club',
    'COMPANY',
    'Historic South Kolkata club preserving classical Indian temple heritage and dazzling Chandannagar illuminations.',
    'https://images.unsplash.com/photo-1576400883215-7083a8429e33?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1576400883215-7083a8429e33?auto=format&fit=crop&w=1200&q=80',
    '15, Ekdalia Rd, Gariahat, Ballygunge, Kolkata, West Bengal 700019',
    'Behind Gariahat Flyover',
    22.5195,
    88.3662,
    '+91-9830667788',
    true,
    '09:00:00',
    '04:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'ekdalia@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'deshapriya-park',
    'Deshapriya Park Durgotsav Samiti',
    'COMPANY',
    'Grand open-air community carnival, devotional musical concerts, and record-setting festival installations in South Kolkata.',
    'https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?auto=format&fit=crop&w=1200&q=80',
    'Deshapriya Park West, Kalighat, Kolkata, West Bengal 700026',
    'Opposite Priya Cinema',
    22.5181,
    88.3533,
    '+91-9830778899',
    true,
    '08:00:00',
    '03:30:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'deshapriya@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'salt-lake-fd-block',
    'FD Block Residents Association',
    'COMPANY',
    'Community welfare organization hosting the renowned Salt Lake FD Block Sharod Mela, food festivals, and entertainment.',
    'https://images.unsplash.com/photo-1513151233558-d860c5398176?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1513151233558-d860c5398176?auto=format&fit=crop&w=1200&q=80',
    'FD Block, Sector 3, Bidhannagar, Kolkata, West Bengal 700091',
    'Near FD Community Hall & Park',
    22.5833,
    88.4145,
    '+91-9830889900',
    true,
    '16:00:00',
    '23:59:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'fdblock@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'bengal-crafts-guild',
    'Bengal Crafts & Heritage Guild',
    'COMPANY',
    'Preserving and presenting rural Bengal art, Dokra metalcraft, terracotta pottery exhibitions, and literary festivals.',
    'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1200&q=80',
    'Central Park, Sector 2, Salt Lake, Kolkata, West Bengal 700091',
    'Near Karunamoyee Bus Terminus',
    22.5882,
    88.4208,
    '+91-9830990011',
    true,
    '13:00:00',
    '22:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'craftsguild@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'bengal-weavers-collective',
    'Bengal Weavers & Artisans Collective',
    'STORE',
    'Direct-from-weaver cooperative showcasing genuine Baluchari, Dhakai Jamdani, Tussar silk sarees, and festive apparel.',
    'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=1200&q=80',
    'Rash Behari Ave, Gariahat Crossing, Kolkata, West Bengal 700019',
    'Near Anandamela Complex',
    22.5186,
    88.3653,
    '+91-9831001122',
    true,
    '10:30:00',
    '21:30:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'weavers@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'new-market-merchants',
    'New Market Merchants Association',
    'STORE',
    'Historic trade collective representing renowned central Kolkata fashion, leather footwear, and lifestyle stores.',
    'https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=1200&q=80',
    'Lindsay St, New Market Area, Dharmatala, Kolkata, West Bengal 700087',
    'Near Simpark Mall Entrance',
    22.5601,
    88.3524,
    '+91-9831112233',
    true,
    '11:00:00',
    '22:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'newmarket@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'kalakriti-boutique',
    'Kalakriti Designer Boutique',
    'STORE',
    'Designer bridalwear, ethnic party ensembles, and festive Indo-Western fashion boutique in Salt Lake City Centre 1.',
    'https://images.unsplash.com/photo-1441984904996-e0b6ba687e04?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1441984904996-e0b6ba687e04?auto=format&fit=crop&w=1200&q=80',
    'DC Block, Sector 1, Bidhannagar, Kolkata, West Bengal 700064',
    'City Centre 1, Kund Area Ground Floor',
    22.5898,
    88.4072,
    '+91-9831223344',
    true,
    '11:00:00',
    '21:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'kalakriti@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'rasogolla-and-co',
    'Rasogolla & Co. Bistro',
    'STORE',
    'Contemporary dessert parlour offering artisanal baked nolen gur rosogollas, Kolkata fusion snacks, and festive delights.',
    'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=1200&q=80',
    '18, Park St, Taltala, Kolkata, West Bengal 700016',
    'Next to Oxford Bookstore',
    22.5532,
    88.3537,
    '+91-9831334455',
    true,
    '12:00:00',
    '23:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'rasogolla@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'kumartuli-artisans',
    'Kumartuli Artisans Association',
    'COMPANY',
    'Sacred traditional sculptors association shaping immortal clay idols and cultural festival artwork in North Kolkata.',
    'https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80',
    '8/B, Abhay Mitra St, Sovabazar, Kumartuli, Kolkata, West Bengal 700005',
    'Near Kumartuli Ghat',
    22.6008,
    88.3631,
    '+91-9831556677',
    true,
    '09:00:00',
    '02:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'kumartuli@organizer.myciti.in'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO client_profiles (
    user_id, slug, business_name, client_type, bio, logo_url, cover_url,
    address, landmark, latitude, longitude, public_phone, show_phone,
    daily_open_time, daily_close_time, verified, status, created_at, updated_at
)
SELECT 
    u.id,
    'myciti-official',
    'myCiti Community Organizers',
    'COMPANY',
    'Verified community organizers and city partners curating authentic events, festival pandals, and local offerings on myCiti.',
    'https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=400&q=80',
    'https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=1200&q=80',
    'Kolkata, West Bengal, India',
    'City Center Hub',
    22.5726,
    88.3639,
    '+91-9830000000',
    true,
    '08:00:00',
    '23:00:00',
    true,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'official@myciti.in'
ON CONFLICT (slug) DO NOTHING;

-- 3. Link each seed listing (1 to 15) to its organizer profile
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'bagbazar-sarbojanin') WHERE id = 1;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'college-square-sarbojanin') WHERE id = 2;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'sreebhumi-sporting-club') WHERE id = 3;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'maddox-square-puja') WHERE id = 4;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'suruchi-sangha') WHERE id = 5;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'ekdalia-evergreen-club') WHERE id = 6;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'deshapriya-park') WHERE id = 7;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'salt-lake-fd-block') WHERE id = 8;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'bengal-crafts-guild') WHERE id = 9;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'bengal-weavers-collective') WHERE id IN (10, 14);
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'new-market-merchants') WHERE id = 11;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'kalakriti-boutique') WHERE id = 12;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'rasogolla-and-co') WHERE id = 13;
UPDATE listings SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'kumartuli-artisans') WHERE id = 15;

-- 4. Backfill any user submission listings if they were approved earlier
UPDATE listings l
SET client_profile_id = cp.id
FROM submissions s
JOIN client_profiles cp ON cp.user_id = s.user_id
WHERE l.source_submission_id = s.id
  AND l.client_profile_id IS NULL;

-- 5. Fallback for any remaining unlinked listings
UPDATE listings l
SET client_profile_id = (SELECT id FROM client_profiles WHERE slug = 'myciti-official')
WHERE l.client_profile_id IS NULL;

-- 6. Keep primary key sequences aligned
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('client_profiles_id_seq', (SELECT COALESCE(MAX(id), 1) FROM client_profiles));
